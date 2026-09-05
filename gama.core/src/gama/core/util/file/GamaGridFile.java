/*******************************************************************************************************
 *
 * GamaGridFile.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.util.file;

import static gama.core.topology.gis.ProjectionFactory.getTargetCRSOrDefault;
import static org.geotools.util.factory.Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.geometry.Position;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.data.PrjFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2DArchived;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;

import gama.annotations.doc;
import gama.annotations.example;
import gama.annotations.file;
import gama.annotations.support.IConcept;
import gama.api.GAMA;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.symbols.Facets;
import gama.api.gaml.types.IType;
import gama.api.gaml.types.Types;
import gama.api.kernel.topology.ICoordinateReferenceSystem;
import gama.api.runtime.scope.IScope;
import gama.api.types.geometry.GamaPointFactory;
import gama.api.types.geometry.GamaShapeFactory;
import gama.api.types.geometry.IPoint;
import gama.api.types.geometry.IShape;
import gama.api.types.list.GamaListFactory;
import gama.api.types.list.IList;
import gama.api.types.matrix.GamaMatrixFactory;
import gama.api.types.matrix.IField;
import gama.api.types.matrix.IMatrix;
import gama.api.ui.IStatusMessage;
import gama.api.utils.geometry.GamaEnvelopeFactory;
import gama.api.utils.geometry.IEnvelope;
import gama.api.utils.interfaces.IFieldMatrixProvider;
import gama.core.topology.gis.GamaCRS;
import gama.core.util.matrix.GamaFloatMatrix;

/**
 * The Class GamaGridFile.
 */
@file (
		name = "grid",
		extensions = { "asc", "tif" },
		buffer_type = IType.LIST,
		buffer_content = IType.GEOMETRY,
		buffer_index = IType.INT,
		concept = { IConcept.GRID, IConcept.ASC, IConcept.TIF, IConcept.FILE },
		doc = @doc ("Represents .asc or .tif files that contain grid descriptions"))
@SuppressWarnings ({ "unchecked", "rawtypes" })
public class GamaGridFile extends GamaGisFile implements IFieldMatrixProvider {

	/**
	 * The Class Records.
	 */
	static class Records {

		/** The x. */
		double x[];

		/** The y. */
		double y[];

		/** The bands. */
		final List<double[]> bands = new ArrayList<>();

		/**
		 * Fill.
		 *
		 * @param i
		 *            the i
		 * @param bands2
		 *            the bands 2
		 */
		public void fill(final int i, final IList<Double> bands2) {
			for (double[] tab : bands) { bands2.add(tab[i]); }
		}
	}

	/** The coverage. */
	transient GridCoverage2D coverage;

	/** The asc data. */
	GamaFloatMatrix ascData;

	/** The asc info. */
	Double[] ascInfo;

	/** The num cols. */
	public int nbBands, numRows, numCols;

	/** The geom. */
	IShape geom;

	/** The no data. */
	Number noData = IField.NO_NO_DATA;

	/** The genv. */
	GeneralBounds genv;

	/** The records. */
	Records records;

	/**
	 * Instantiates a new gama grid file.
	 *
	 * @param scope
	 *            the scope
	 * @param pathName
	 *            the path name
	 * @throws GamaRuntimeException
	 *             the gama runtime exception
	 */
	@doc (
			value = "This file constructor allows to read a asc file or a tif (geotif) file",
			examples = { @example (
					value = "file f <- grid_file(\"file.asc\");",
					isExecutable = false) })

	public GamaGridFile(final IScope scope, final String pathName) throws GamaRuntimeException {
		super(scope, pathName, (Integer) null);
	}

	/**
	 * Instantiates a new gama grid file.
	 *
	 * @param scope
	 *            the scope
	 * @param pathName
	 *            the path name
	 * @param asMatrix
	 *            the as matrix
	 * @throws GamaRuntimeException
	 *             the gama runtime exception
	 */
	@doc (
			value = "This file constructor allows to read a asc file or a tif (geotif) file, but without converting it into shapes. Only a matrix of float values is created",
			examples = { @example (
					value = "file f <- grid_file(\"file.asc\", false);",
					isExecutable = false) })

	public GamaGridFile(final IScope scope, final String pathName, final boolean asMatrix) throws GamaRuntimeException {
		super(scope, pathName, (Integer) null);
	}

	/**
	 * Instantiates a new gama grid file.
	 *
	 * @param scope
	 *            the scope
	 * @param pathName
	 *            the path name
	 * @param code
	 *            the code
	 * @throws GamaRuntimeException
	 *             the gama runtime exception
	 */
	@doc (
			value = "This file constructor allows to read a asc file or a tif (geotif) file specifying the coordinates system code, as an int (epsg code)",
			examples = { @example (
					value = "file f <- grid_file(\"file.asc\", 32648);",
					isExecutable = false) })
	public GamaGridFile(final IScope scope, final String pathName, final Integer code) throws GamaRuntimeException {
		super(scope, pathName, code);
	}

	/**
	 * Instantiates a new gama grid file.
	 *
	 * @param scope
	 *            the scope
	 * @param pathName
	 *            the path name
	 * @param code
	 *            the code
	 */
	@doc (
			value = "This file constructor allows to read a asc file or a tif (geotif) file specifying the coordinates system code (epg,...,), as a string ",
			examples = { @example (
					value = "file f <- grid_file(\"file.asc\",\"EPSG:32648\");",
					isExecutable = false) })
	public GamaGridFile(final IScope scope, final String pathName, final String code) {
		super(scope, pathName, code);
	}

	/**
	 * Instantiates a new gama grid file.
	 *
	 * @param scope
	 *            the scope
	 * @param pathName
	 *            the path name
	 * @param field
	 *            the field
	 */
	@doc (
			value = "This allows to build a writable grid file from the values of a field",
			examples = { @example (
					value = "file f <- grid_file(\"file.tif\",my_field); save f;",
					isExecutable = false) })
	public GamaGridFile(final IScope scope, final String pathName, final IField field) {
		super(scope, pathName, false);
		setWritable(scope, true);
		createCoverage(scope, field);
	}

	@Override
	public IList<String> getAttributes(final IScope scope) {
		// No attributes
		return GamaListFactory.getEmptyList();
	}

	/**
	 * Creates the coverage.
	 *
	 * @param scope
	 *            the scope
	 */
	private void createCoverage(final IScope scope) {
		if (coverage == null) {
			final File gridFile = getFile(scope);
			gridFile.setReadable(true);
			InputStream fis = null;
			try {
				fis = Files.newInputStream(gridFile.toPath());
			} catch (IOException e1) {}
			try {
				privateCreateCoverage(scope, fis);
			} catch (final Throwable e) {
				// A problem appeared, likely related to the wrong format of the file (see Issue 412).
				// On Android the GeoTools/imageio TIFF stack is not fully available, so a TIFF
				// read failure must fall back to a direct TIFF parser instead of a hard error.
				if (isTiff(scope)) {
					customTiffReader(scope);
				} else {
					customAscReader(scope);
				}
				/*
				 * try { fis = fixFileHeader(scope); } catch (UnsupportedEncodingException e2) { e2.printStackTrace(); }
				 * try { privateCreateCoverage(scope, fis); } catch (IOException e1) { e1.printStackTrace(); }
				 */
			}
		}
	}

	/**
	 * Double val.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param line
	 *            the line
	 * @return the double
	 * @date 31 août 2023
	 */
	private Double doubleVal(final String line) {
		String[] l = line.split(" ");
		if (l.length == 1) { l = line.split("t"); }
		if (l.length > 1) return Double.valueOf(l[l.length - 1]);
		return null;
	}

	/**
	 * Int val.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param line
	 *            the line
	 * @return the integer
	 * @date 31 août 2023
	 */
	private Integer intVal(final String line) {

		String[] l = line.split(" ");
		if (l.length == 1) { l = line.split("t"); }
		if (l.length > 1) return Integer.valueOf(l[l.length - 1]);
		return null;
	}

	/**
	 * Custom asc reader.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param scope
	 *            the scope
	 * @date 31 août 2023
	 */
	private void customAscReader(final IScope scope) {
		try (Scanner scanner = new Scanner(getFile(scope))) {
			boolean headingComplete = false;
			Integer nbCols = null;
			Integer nbRows = null;
			Double xCorner = null;
			Double yCorner = null;
			Double xCenter = null;
			Double yCenter = null;
			Double dX = null;
			Double dY = null;
			Double noDataD = null;
			ascInfo = new Double[4];
			int j = 0;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				line = line.toLowerCase();
				if (!headingComplete) {
					if (dX == null && line.contains("dx")) {
						dX = doubleVal(line);
						ascInfo[0] = dX;
					} else if (dY == null && line.contains("dy")) {
						dY = doubleVal(line);
						ascInfo[1] = dY;
					} else if ((dX == null || dY == null) && line.contains("cellsize")) {
						Double cellSize = doubleVal(line);
						if (dX == null) {
							dX = cellSize;
							ascInfo[0] = dX;
						}
						if (dY == null) {
							dY = cellSize;
							ascInfo[1] = dY;
						}
					} else if (nbCols == null && line.contains("ncols")) {
						nbCols = intVal(line);
					} else if (nbRows == null && line.contains("nrows")) {
						nbRows = intVal(line);
					} else if (noDataD == null && (line.contains("nodata") || line.contains("nodata_value"))) {
						noDataD = line.contains("nan") ? Double.NaN : doubleVal(line);
					} else if (xCorner == null && xCenter == null && line.contains("xllcorner")) {
						xCorner = doubleVal(line);
						ascInfo[2] = xCorner;
					} else if (yCorner == null && yCenter == null && line.contains("yllcorner")) {
						yCorner = doubleVal(line);
					} else if (xCorner == null && xCenter == null && line.contains("xllcenter")) {
						xCenter = doubleVal(line);
					} else if (yCorner == null && yCenter == null && line.contains("yllcenter")) {
						yCenter = doubleVal(line);
					} else if (line.replace(" ", "").length() > 0) {
						if (nbCols == null || nbCols == 0 || nbRows == null || nbRows == 0)
							throw GamaRuntimeException.error("The format of " + getName(scope)
									+ " is not correct. Error: NCOLS and NROWS have to be defined", scope);
						if (xCenter != null && dX != null) {
							xCorner = xCenter - nbCols * dX / 2.0;
							ascInfo[2] = xCorner;
						}
						if (yCenter != null && dY != null) { yCorner = yCenter - nbRows * dY / 2.0; }

						if (yCorner != null && dY != null) { ascInfo[3] = yCorner + nbRows * dY; }

						ascData = (GamaFloatMatrix) GamaMatrixFactory.createFloatMatrix(nbCols, nbRows);
						if (noData != null) { this.noData = noDataD; }
						double xC = xCorner == null ? 0 : xCorner;
						double yC = yCorner == null ? 0 : yCorner;
						final IEnvelope env =
								GamaEnvelopeFactory.of(xC, xC + nbCols * (dX == null ? 0 : dX), yC, ascInfo[3], 0, 0);
						computeProjection(scope, env);
						numRows = nbRows;
						numCols = nbCols;

						headingComplete = true;
					}
				}
				if (headingComplete) {
					String[] l = line.split(" ");
					for (int i = 0; i < l.length; i++) {
						if (l[i].isEmpty()) { continue; }
						if (noDataD != null && noDataD.isNaN()) {
							Double v = 0.0;
							try {
								v = Double.valueOf(l[i]);
							} catch (Exception e) {
								v = Double.NaN;
							}
							ascData.set(scope, i, j, v);
						} else {
							ascData.set(scope, i, j, Double.valueOf(l[i]));
						}

					}
					j++;
				}
			}
		} catch (final FileNotFoundException e2) {
			throw GamaRuntimeException
					.error("The format of " + getName(scope) + " is not correct. Error: " + e2.getMessage(), scope);
		}

	}

	/**
	 * Custom TIFF reader.
	 *
	 * Fallback used when the GeoTools/imageio TIFF stack is unavailable (e.g. on Android, where the
	 * <code>it.geosolutions.imageioimpl.plugins.tiff</code> plugins and the standard
	 * <code>javax.imageio</code> implementation are not fully present). Supports uncompressed, PackBits,
	 * Deflate and LZW compressed single-band byte/ushort/float samples with BlackIsZero/WhiteIsZero
	 * photometric interpretation, mirroring the {@link #customAscReader(IScope)} fallback (results are
	 * stored in {@link #ascData}/{@link #ascInfo} and used as a float matrix).
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr) / Android port
	 * @date 05 septembre 2026
	 */
	private void customTiffReader(final IScope scope) {
		try (RandomAccessFile raf = new RandomAccessFile(getFile(scope), "r")) {
			if (raf.length() < 8) throw new IOException("File too small to be a TIFF");

			final boolean little;
			raf.seek(0);
			int b0 = raf.readUnsignedByte();
			int b1 = raf.readUnsignedByte();
			if (b0 == 'I' && b1 == 'I') {
				little = true;
			} else if (b0 == 'M' && b1 == 'M') {
				little = false;
			} else {
				throw new IOException("Not a TIFF file (bad byte-order marker)");
			}
			if (tiffShort(raf, little) != 42) throw new IOException("Not a TIFF file (bad magic number)");

			long ifdOffset = tiffLong(raf, little);
			if (ifdOffset >= raf.length()) throw new IOException("Bad IFD offset");

			int width = 0, height = 0, rowsPerStrip = 0, compression = 1;
			int bitsPerSample = 8, samplesPerPixel = 1, sampleFormat = 1, photo = 1, planar = 1;
			long[] stripOffsets = null, stripByteCounts = null;
			double[] pixelScale = null, tiePoint = null;
			Double tiffNoData = null;

			raf.seek(ifdOffset);
			int nbEntries = tiffShort(raf, little);
			for (int i = 0; i < nbEntries; i++) {
				long entryStart = raf.getFilePointer();
				int tag = tiffShort(raf, little);
				int type = tiffShort(raf, little);
				long count = tiffLong(raf, little);
				long valueField = tiffLong(raf, little);
				double[] values = tiffValues(raf, little, type, count, valueField, entryStart);
				switch (tag) {
					case 256: if (values.length > 0) width = (int) values[0]; break;
					case 257: if (values.length > 0) height = (int) values[0]; break;
					case 258: if (values.length > 0) bitsPerSample = (int) values[0]; break;
					case 259: if (values.length > 0) compression = (int) values[0]; break;
					case 262: if (values.length > 0) photo = (int) values[0]; break;
					case 273: stripOffsets = toLongArray(values); break;
					case 277: if (values.length > 0) samplesPerPixel = (int) values[0]; break;
					case 278: if (values.length > 0) rowsPerStrip = (int) values[0]; break;
					case 279: stripByteCounts = toLongArray(values); break;
					case 284: if (values.length > 0) planar = (int) values[0]; break;
					case 339: if (values.length > 0) sampleFormat = (int) values[0]; break;
					case 33550: pixelScale = values; break;
					case 33922: tiePoint = values; break;
					case 42113: if (values.length > 0) tiffNoData = values[0]; break;
					default: break;
				}
				raf.seek(entryStart + 12);
			}

			if (width < 1 || height < 1) throw new IOException("TIFF without a valid image size");
			if (samplesPerPixel != 1) throw new IOException("Only single-band (grayscale) GeoTIFF files are supported");
			if (planar != 1) throw new IOException("Planar TIFF files are not supported");
			if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 32)
				throw new IOException("Unsupported BitsPerSample: " + bitsPerSample);
			if (stripOffsets == null || stripByteCounts == null)
				throw new IOException("TIFF has no strip/tile layout");
			if (compression != 1 && compression != 5 && compression != 8 && compression != 32773)
				throw new IOException("Unsupported TIFF compression: " + compression);

			numCols = width;
			numRows = height;
			ascData = (GamaFloatMatrix) GamaMatrixFactory.createFloatMatrix(width, height);
			if (tiffNoData != null) { this.noData = tiffNoData; }

			final int bytesPerSample = bitsPerSample / 8;
			final int bytesPerPixel = bytesPerSample * samplesPerPixel;

			// Decide how to slice the raster into strips. Most writers store one
			// offset/count per strip; some store a single strip for the whole image.
			if (stripOffsets.length == 1 && stripByteCounts.length == 1
					&& (rowsPerStrip <= 0 || rowsPerStrip >= height)) {
				decodeTiffStrip(raf, scope, stripOffsets[0], stripByteCounts[0], compression, 0, height,
						bytesPerPixel, sampleFormat, photo);
			} else {
				final int strips = stripOffsets.length;
				for (int s = 0; s < strips; s++) {
					int firstRow = s * rowsPerStrip;
					if (firstRow >= height) break;
					int thisRows = Math.min(rowsPerStrip, height - firstRow);
					if (thisRows <= 0) break;
					decodeTiffStrip(raf, scope, stripOffsets[s], stripByteCounts[s], compression, firstRow, thisRows,
							bytesPerPixel, sampleFormat, photo);
				}
			}

			// Envelope from the georeferencing tags (ModelTiepoint / ModelPixelScale).
			// The tiepoint carries absolute projected coordinates; feeding them (in metres)
			// through the CRS machinery degenerates the world projection on Android (the
			// EPSG WKT database is limited there), producing a void envelope and invisible
			// cells. Since GAMA's WorldProjection always re-anchors the world at its own
			// projected origin anyway, we normalize the envelope to the local origin so the
			// cells (and the model camera, expressed in that local space) line up exactly
			// as they do on the desktop where the same normalization happens.
			double sx = 1, sy = 1, xCorner = 0, maxY = height;
			if (pixelScale != null && pixelScale.length >= 2 && tiePoint != null && tiePoint.length >= 2) {
				sx = pixelScale[0] == 0 ? 1 : pixelScale[0];
				sy = pixelScale[1] == 0 ? 1 : pixelScale[1];
				if (sy < 0) sy = -sy;
				xCorner = 0d;
				maxY = height * sy;
				ascInfo = new Double[] { sx, sy, xCorner, maxY };
				final IEnvelope env = GamaEnvelopeFactory.of(0, width * sx, 0, height * sy, 0, 0);
				computeProjection(scope, env);
			} else {
				ascInfo = new Double[] { 1d, 1d, 0d, (double) height };
				final IEnvelope env = GamaEnvelopeFactory.of(0, width, 0, height, 0, 0);
				computeProjection(scope, env);
			}
		} catch (final Throwable e) {
			throw GamaRuntimeException
					.error("The format of " + getName(scope) + " seems incorrect: " + e.getMessage(), scope);
		}
	}

	/**
	 * Reads and stores one TIFF strip into {@link #ascData}: seeks to the strip offset, decompresses its
	 * bytes, then copies the raster values into rows <code>[firstRow, firstRow+numRows)</code> (rows are
	 * stored top-first in a TIFF, like the ASC fallback expects).
	 */
	private void decodeTiffStrip(final RandomAccessFile raf, final IScope scope, final long offset, final long byteCount,
			final int compression, final int firstRow, final int numRows, final int bytesPerPixel,
			final int sampleFormat, final int photo) throws IOException {
		if (byteCount <= 0 || offset + byteCount > raf.length()) throw new IOException("Bad TIFF strip offset/length");
		long expectedL = (long) numCols * numRows * bytesPerPixel;
		if (expectedL <= 0 || expectedL > Integer.MAX_VALUE - 8) throw new IOException("TIFF strip too large");
		int expected = (int) expectedL;
		byte[] raw = new byte[(int) byteCount];
		raf.seek(offset);
		raf.readFully(raw);
		byte[] px = tiffDecompress(raw, compression, expected);

		for (int r = 0; r < numRows; r++) {
			for (int c = 0; c < numCols; c++) {
				int idx = (r * numCols + c) * bytesPerPixel;
				double v = tiffSample(px, idx, bytesPerPixel, sampleFormat, photo);
				ascData.set(scope, c, firstRow + r, v);
			}
		}
	}

	/** Extracts a single sample value (grayscale) from the decoded strip buffer. */
	private static double tiffSample(final byte[] px, final int idx, final int bytesPerPixel, final int sampleFormat,
			final int photo) {
		double v;
		switch (bytesPerPixel) {
			case 2:
				int s = ((px[idx] & 0xFF) << 8) | (px[idx + 1] & 0xFF);
				v = sampleFormat == 2 ? (short) s : s;
				break;
			case 4:
				int i = (px[idx] & 0xFF) << 24 | (px[idx + 1] & 0xFF) << 16 | (px[idx + 2] & 0xFF) << 8
						| (px[idx + 3] & 0xFF);
				v = sampleFormat == 3 ? Float.intBitsToFloat(i) : i;
				break;
			default:
				v = px[idx] & 0xFF;
				break;
		}
		return photo == 0 ? 255 - v : v;
	}

	private static long[] toLongArray(final double[] vs) {
		if (vs == null) return null;
		final long[] out = new long[vs.length];
		for (int i = 0; i < vs.length; i++) { out[i] = (long) vs[i]; }
		return out;
	}

	/**
	 * True when the throwable (or its cause chain) signals a missing class/library on the
	 * classpath (e.g. GeoTiFF/imageio-ext not bundled on Android) rather than a genuine
	 * read failure. Such errors are irrelevant to the caller, which has its own fallback.
	 */
	private static boolean isMissingClass(final Throwable t) {
		for (Throwable cur = t; cur != null; cur = cur.getCause()) {
			if (cur instanceof LinkageError || cur instanceof ClassNotFoundException) return true;
		}
		return false;
	}

	/** Reads 2 bytes at the current stream position in the given byte order. */
	private static int tiffShort(final RandomAccessFile raf, final boolean little) throws IOException {
		if (little) {
			return (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8));
		}
		return raf.readUnsignedShort();
	}

	/** Reads 4 bytes (unsigned) at the current stream position in the given byte order. */
	private static long tiffLong(final RandomAccessFile raf, final boolean little) throws IOException {
		if (little) {
			return (tiffShort(raf, true) | ((long) tiffShort(raf, true) << 16));
		}
		return raf.readInt() & 0xFFFFFFFFL;
	}

	/** Reads 4 bytes (signed) at the current stream position in the given byte order. */
	private static int tiffInt(final RandomAccessFile raf, final boolean little) throws IOException {
		return (int) tiffLong(raf, little);
	}

	/** Reads 8 bytes (double) at the current stream position in the given byte order. */
	private static double tiffDouble(final RandomAccessFile raf, final boolean little) throws IOException {
		final byte[] b = new byte[8];
		raf.readFully(b);
		if (little) {
			for (int i = 0, j = 7; i < j; i++, j--) {
				byte tmp = b[i];
				b[i] = b[j];
				b[j] = tmp;
			}
		}
		return Double.longBitsToDouble(((long) (b[0] & 0xFF) << 56) | ((long) (b[1] & 0xFF) << 48)
				| ((long) (b[2] & 0xFF) << 40) | ((long) (b[3] & 0xFF) << 32) | ((b[4] & 0xFF) << 24)
				| ((b[5] & 0xFF) << 16) | ((b[6] & 0xFF) << 8) | (b[7] & 0xFF));
	}

	private static int tiffTypeSize(final int type) {
		switch (type) {
			case 1: case 2: case 6: case 7: return 1;
			case 3: case 8: return 2;
			case 4: case 9: case 11: case 13: return 4;
			case 5: case 10: case 12: return 8;
			default: return 1;
		}
	}

	/**
	 * Reads the values of a TIFF IFD entry. Inline (<= 4 bytes) values are read from the entry's own value
	 * field; larger ones are read at the offset stored there.
	 */
	private static double[] tiffValues(final RandomAccessFile raf, final boolean little, final int type,
			final long count, final long valueField, final long entryStart) throws IOException {
		final int size = Math.max(1, tiffTypeSize(type));
		final long total = size * count;
		if (count > 1_000_000L) { return new double[0]; }
		final long saved = raf.getFilePointer();
		if (total > 4) {
			raf.seek(valueField);
		} else {
			raf.seek(entryStart + 8);
		}
		final double[] out = new double[(int) Math.min(count, Integer.MAX_VALUE)];
		for (int k = 0; k < out.length; k++) {
			switch (type) {
				case 1: case 2: case 6: case 7:
					out[k] = raf.readUnsignedByte();
					break;
				case 3:
					out[k] = tiffShort(raf, little);
					break;
				case 4:
					out[k] = tiffLong(raf, little);
					break;
				case 8:
					out[k] = (short) tiffShort(raf, little);
					break;
				case 9:
					out[k] = tiffInt(raf, little);
					break;
				case 11:
					out[k] = Float.intBitsToFloat(tiffInt(raf, little));
					break;
				case 12:
					out[k] = tiffDouble(raf, little);
					break;
				case 5: {
					long num = tiffLong(raf, little);
					long den = tiffLong(raf, little);
					out[k] = den == 0 ? 0 : (double) num / den;
					break;
				}
				default:
					raf.skipBytes(size);
					out[k] = 0;
					break;
			}
		}
		raf.seek(saved);
		return out;
	}

	/** Decompresses a TIFF strip according to its Compression tag. */
	private static byte[] tiffDecompress(final byte[] source, final int compression, final int expected)
			throws IOException {
		switch (compression) {
			case 1: // none
				if (source.length < expected) throw new IOException("Strip shorter than expected");
				return Arrays.copyOf(source, expected);
			case 32773: // PackBits
				return tiffPackBits(source, expected);
			case 8: { // Deflate / AdobeDeflate
				Inflater zlib = new Inflater();
				Inflater raw = new Inflater(true);
				try {
					return tiffInflate(source, expected, zlib, raw);
				} finally {
					zlib.end();
					raw.end();
				}
			}
			case 5: // LZW (MSB-first, early code-size change)
				return tiffLzw(source, expected);
			default:
				throw new IOException("Unsupported TIFF compression: " + compression);
		}
	}

	private static byte[] tiffInflate(final byte[] source, final int expected, final Inflater zlib,
			final Inflater raw) throws IOException {
		for (Inflater inflater : new Inflater[] { zlib, raw }) {
			inflater.reset();
			inflater.setInput(source);
			final byte[] out = new byte[expected];
			int done = 0;
			try {
				while (!inflater.finished() && done < expected) {
					int n = inflater.inflate(out, done, expected - done);
					if (n <= 0) break;
					done += n;
				}
			} catch (Exception e) {
				continue;
			}
			if (done == expected) return out;
		}
		throw new IOException("Invalid Deflate data");
	}

	/** PackBits (TIFF flavor) decoding. */
	private static byte[] tiffPackBits(final byte[] source, final int expected) throws IOException {
		final byte[] out = new byte[expected];
		int si = 0, oi = 0;
		while (si < source.length && oi < expected) {
			int n = source[si++];
			if (n >= 0) {
				int cnt = n + 1;
				if (si + cnt > source.length) throw new IOException("Invalid PackBits data");
				for (int k = 0; k < cnt && oi < expected; k++) { out[oi++] = source[si++]; }
			} else if (n != -128) {
				int cnt = 1 - n;
				if (si >= source.length) throw new IOException("Invalid PackBits data");
				byte b = source[si++];
				for (int k = 0; k < cnt && oi < expected; k++) { out[oi++] = b; }
			}
		}
		if (oi < expected) throw new IOException("PackBits data shorter than expected");
		return out;
	}

	/** MSB-first LZW (TIFF flavor) decoding with early code-size change. */
	private static byte[] tiffLzw(final byte[] source, final int expected) throws IOException {
		final java.util.List<byte[]> table = new ArrayList<>();
		for (int i = 0; i < 256; i++) { table.add(new byte[] { (byte) i }); }
		table.add(null); // code 256 = CLEAR
		table.add(null); // code 257 = EOI
		int codeSize = 9;
		int prev = -1;
		final byte[] out = new byte[expected];
		int oi = 0;
		int bitPos = 0;
		boolean eof = false;

		while (!eof && oi < expected) {
			int code = 0;
			for (int b = 0; b < codeSize; b++) {
				int bit;
				if (bitPos >> 3 >= source.length) {
					eof = true;
					break;
				}
				bit = (source[bitPos >> 3] >> (7 - (bitPos & 7))) & 1;
				bitPos++;
				code = (code << 1) | bit;
			}
			if (eof) break;
			if (code == 256) { // CLEAR
				table.subList(258, table.size()).clear();
				codeSize = 9;
				prev = -1;
				continue;
			}
			if (code == 257) break; // EOI
			byte[] entry;
			if (code < table.size() && table.get(code) != null) {
				entry = table.get(code);
			} else if (code == table.size() && prev >= 0) {
				byte[] p = table.get(prev);
				entry = new byte[p.length + 1];
				System.arraycopy(p, 0, entry, 0, p.length);
				entry[p.length] = p[0];
			} else {
				throw new IOException("Invalid LZW code " + code);
			}
			for (byte v : entry) {
				if (oi < expected) out[oi++] = v;
			}
			if (prev >= 0 && table.size() < 4096) {
				byte[] p = table.get(prev);
				byte[] combined = new byte[p.length + 1];
				System.arraycopy(p, 0, combined, 0, p.length);
				combined[p.length] = entry[0];
				table.add(combined);
				if (table.size() == (1 << codeSize) - 1 && codeSize < 12) { codeSize++; }
			}
			prev = code;
		}
		if (oi < expected) throw new IOException("LZW data shorter than expected");
		return out;
	}

	/**
	 * Creates the coverage.
	 *
	 * @param scope
	 *            the scope
	 * @param field
	 *            the field
	 */
	private void createCoverage(final IScope scope, final IField field) {
		// temporary fixes #3128 - the code comes from the save statement... maybe we can do better

		// old code
		/*
		 * double[] data = field.getMatrix();
		 *
		 * DataBuffer buffer = new DataBufferDouble(data, data.length); SampleModel sample = new
		 * BandedSampleModel(DataBuffer.TYPE_DOUBLE, field.numCols, field.numRows, field.getBandsNumber(scope));
		 * WritableRaster raster = Raster.createWritableRaster(sample, buffer, null); Envelope2D envelope = new
		 * Envelope2D(getCRS(scope), 0, 0, scope.getSimulation().getWidth(), scope.getSimulation().getHeight());
		 * GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null); GridCoverage2D cov =
		 * factory.create(getName(scope), raster, envelope); coverage = cov;
		 */
		final boolean nullProjection = scope.getSimulation().getProjectionFactory().getWorld() == null;

		final int cols = field.getCols(scope);
		final int rows = field.getRows(scope);
		double x = nullProjection ? 0
				: scope.getSimulation().getProjectionFactory().getWorld().getProjectedEnvelope().getMinX();
		double y = nullProjection ? 0
				: scope.getSimulation().getProjectionFactory().getWorld().getProjectedEnvelope().getMinY();

		final float[][] imagePixelData = new float[rows][cols];
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) { imagePixelData[row][col] = field.get(scope, col, row).floatValue(); }

		}
		final double width = scope.getSimulation().getEnvelope().getWidth();
		final double height = scope.getSimulation().getEnvelope().getHeight();

		Envelope2DArchived refEnvelope =
				new Envelope2DArchived(getTargetCRSOrDefault(scope).getCRS(), x, y, width, height);

		coverage = new GridCoverageFactory().create("data", imagePixelData, refEnvelope);

	}

	@Override
	protected void flushBuffer(final IScope scope, final Facets facets) throws GamaRuntimeException {
		if (!writable || coverage == null) return;
		try {
			final File f = getFile(scope);
			f.setWritable(true);
			GridCoverageWriter writer;

			if (isTiff(scope)) {
				final GeoTiffFormat format = new GeoTiffFormat();
				writer = format.getWriter(f);
			} else {
				writer = new ArcGridWriter(f);
			}
			writer.write(coverage, (GeneralParameterValue[]) null);
		} catch (final IOException e) {
			throw GamaRuntimeException.create(e, scope);
		}
	}

	/**
	 * Private create coverage.
	 *
	 * @param scope
	 *            the scope
	 * @param fis
	 *            the fis
	 * @throws DataSourceException
	 *             the data source exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void privateCreateCoverage(final IScope scope, final InputStream fis) {
		AbstractGridCoverage2DReader store = null;
		try {
			final ICoordinateReferenceSystem crs = getExistingCRS(scope);
			CoordinateReferenceSystem geoToolsCrs = crs == null ? null : crs.getCRS();
			Hints hints = new Hints();
			hints.put(Hints.SKIP_EXTERNAL_OVERVIEWS, Boolean.TRUE);

			if (geoToolsCrs != null) { hints.put(DEFAULT_COORDINATE_REFERENCE_SYSTEM, geoToolsCrs); }

			if (isTiff(scope)) {
				// If no CRS resolved yet, try extracting GeoKeys EPSG directly
				if (geoToolsCrs == null) {
					Integer epsgCode = extractEPSGCode(getFile(scope));
					if (epsgCode != null) {
						try {
							geoToolsCrs = CRS.decode("EPSG:" + epsgCode);
							hints.put(DEFAULT_COORDINATE_REFERENCE_SYSTEM, geoToolsCrs);
						} catch (Throwable ignored) {}
					}
				}

				try {
					store = new GeoTiffReader(getFile(scope), hints);
				} catch (Throwable e) {
					// Hard fallback
					Integer epsgCode = extractEPSGCode(getFile(scope));
					if (epsgCode == null) throw e;
					hints.put(DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:" + epsgCode));
					store = new GeoTiffReader(getFile(scope), hints);
				}
				noData = ((GeoTiffReader) store).getMetadata().getNoData();
			} else {
				store = new ArcGridReader(fis, hints);
			}
			genv = store.getOriginalEnvelope();
			final IEnvelope env = GamaEnvelopeFactory.of(genv.getMinimum(0), genv.getMaximum(0), genv.getMinimum(1),
					genv.getMaximum(1), 0, 0);
			computeProjection(scope, env);
			numRows = store.getOriginalGridRange().getHigh(1) + 1;
			numCols = store.getOriginalGridRange().getHigh(0) + 1;
			coverage = store.read(null);
		} catch (Throwable e) {
			throw GamaRuntimeException.create(e, scope);
		} finally {
			if (store != null) { store.dispose(); }
			scope.getGui().getStatus().endTask("Opening file " + getName(scope), IStatusMessage.DOWNLOAD_ICON);
		}
	}

	/**
	 * Gets the value.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param scope
	 *            the scope
	 * @param locX
	 *            the loc X
	 * @param locY
	 *            the loc Y
	 * @param i
	 *            the i
	 * @param j
	 *            the j
	 * @return the value
	 * @date 31 août 2023
	 */
	private double[] getValue(final IScope scope, final Double locX, final Double locY, final int i, final int j) {
		if (coverage != null) return coverage.evaluate((Position) new Position2D(locX, locY), (double[]) null);
		double[] v = new double[1];
		v[0] = ascData.get(scope, i, j);
		return v;
	}

	/**
	 * Read.
	 *
	 * @param scope
	 *            the scope
	 * @param readAll
	 *            the read all
	 * @param createGeometries
	 *            the create geometries
	 */
	void read(final IScope scope, final boolean readAll, final boolean createGeometries) {

		try {
			String task = "Reading file " + getName(scope);
			scope.getGui().getStatus().beginTask(task, IStatusMessage.DOWNLOAD_ICON);
			final IEnvelope envP = gis == null ? scope.getSimulation().getEnvelope() : gis.getProjectedEnvelope();
			if (gis != null && !(gis.getInitialCRS(scope).getCRS() instanceof ProjectedCRS)) {
				GAMA.reportError(scope, GamaRuntimeException.warning("Try to project a grid -" + this.originalPath
						+ "-  that is not projected. Projection of grids can lead to errors in the cell coordinates. ",
						scope), false);
			}
			final double cellHeight = envP.getHeight() / numRows;
			final double cellWidth = envP.getWidth() / numCols;
			final IList<IShape> shapes = GamaListFactory.create(Types.GEOMETRY);
			final double originX = envP.getMinX();
			final double originY = envP.getMinY();
			final double maxY = envP.getMaxY();
			final double maxX = envP.getMaxX();
			shapes.add(GamaPointFactory.create(originX, originY));
			shapes.add(GamaPointFactory.create(maxX, originY));
			shapes.add(GamaPointFactory.create(maxX, maxY));
			shapes.add(GamaPointFactory.create(originX, maxY));
			shapes.add(shapes.get(0));
			geom = GamaShapeFactory.buildPolygon(shapes);
			if (!readAll) return;

			final double cmx = cellWidth / 2;
			final double cmy = cellHeight / 2;
			double cellHeightP;
			double cellWidthP;
			double originXP;
			double maxYP;
			if (genv != null) {
				cellHeightP = genv.getSpan(1) / numRows;
				cellWidthP = genv.getSpan(0) / numCols;
				originXP = genv.getMinimum(0);
				maxYP = genv.getMaximum(1);

			} else {
				cellHeightP = ascInfo[1];
				cellWidthP = ascInfo[0];
				originXP = ascInfo[2];
				maxYP = ascInfo[3];
			}
			final double cmxP = cellWidthP / 2;
			final double cmyP = cellHeightP / 2;

			if (records == null) {
				records = new Records();
				records.x = new double[numRows * numCols]; // x
				records.y = new double[numRows * numCols]; // y
				records.bands.add(new double[numRows * numCols]); // data
				for (int i = 0, n = numRows * numCols; i < n; i++) {
					scope.getGui().getStatus().setTaskCompletion(task, i / (double) n);

					final int yy = i / numCols;
					final int xx = i - yy * numCols;

					records.x[i] = originX + xx * cellWidth + cmx;
					records.y[i] = maxY - (yy * cellHeight + cmy);

					double[] vd = getValue(scope, originXP + xx * cellWidthP + cmxP, maxYP - (yy * cellHeightP + cmyP),
							xx, yy);
					nbBands = vd.length;
					if (i == 0 && vd.length > 1) {
						for (int j = 0; j < vd.length - 1; j++) { records.bands.add(new double[numRows * numCols]); }
					}
					for (int j = 0; j < vd.length; j++) { records.bands.get(j)[i] = vd[j]; }

				}
			}
			if (getBuffer() == null && createGeometries) {
				// Building geometries
				for (int i = 0, n = numRows * numCols; i < n; i++) {
					setBuffer(GamaListFactory.<IShape> create(Types.GEOMETRY));
					final IPoint p = GamaPointFactory.create(records.x[i], records.y[i]);
					IShape rect = GamaShapeFactory.buildRectangle(cellWidth, cellHeight, p);
					if (gis == null) {
						rect = GamaShapeFactory.createFrom(rect.getInnerGeometry());
					} else {
						rect = GamaShapeFactory.createFrom(gis.transform(rect.getInnerGeometry()));
					}
					IList<Double> bands = GamaListFactory.create(scope, Types.FLOAT);
					records.fill(i, bands);
					rect.setAttribute("grid_value", bands.get(0));
					rect.setAttribute("bands", bands);
					getBuffer().add(rect);
				}
			}
		} catch (final Exception e) {
			throw GamaRuntimeException
					.error("The format of " + getName(scope) + " is not correct. Error: " + e.getMessage(), scope);
		} finally {
			scope.getGui().getStatus().endTask("Reading file " + getName(scope), IStatusMessage.DOWNLOAD_ICON);
		}

	}

	@Override
	public IEnvelope computeEnvelope(final IScope scope) {
		if (gis == null) { createCoverage(scope); }
		return gis.getProjectedEnvelope();
		// OLD : see what it changes to not do it
		// fillBuffer(scope);
		// return gis.getProjectedEnvelope();
	}

	@Override
	protected void fillBuffer(final IScope scope) {
		if (getBuffer() != null) return;
		createCoverage(scope);
		read(scope, true, true);
	}

	/**
	 * Gets the nb rows.
	 *
	 * @param scope
	 *            the scope
	 * @return the nb rows
	 */
	public int getNbRows(final IScope scope) {
		createCoverage(scope);
		return numRows;
	}

	/**
	 * Checks if is tiff.
	 *
	 * @param scope
	 *            the scope
	 * @return true, if is tiff
	 */
	public boolean isTiff(final IScope scope) {
		return getExtension(scope).startsWith("tif");
	}

	@Override
	public IShape getGeometry(final IScope scope) {
		createCoverage(scope);
		read(scope, false, false);
		return geom;
	}

	@Override
	protected ICoordinateReferenceSystem getOwnCRS(final IScope scope) {
		final File source = getFile(scope);
		final String sourceAsString = source.getAbsolutePath();
		final int index = sourceAsString.lastIndexOf('.');
		final StringBuilder prjFileName;
		if (index == -1) {
			prjFileName = new StringBuilder(sourceAsString);
		} else {
			prjFileName = new StringBuilder(sourceAsString.substring(0, index));
		}
		prjFileName.append(".prj");

		// does it exist?
		final File prjFile = new File(prjFileName.toString());
		if (prjFile.exists()) {
			// it exists then we have to read it
			try (FileInputStream fip = new FileInputStream(prjFile);
					final FileChannel channel = fip.getChannel();
					PrjFileReader projReader = new PrjFileReader(channel);) {
				return new GamaCRS(projReader.getCoordinateReferenceSystem());
			} catch (final IOException | FactoryException e) {
				// warn about the error but proceed, it is not fatal
				// we have at least the default crs to use
				return null;
			}
		}
		if (isTiff(scope)) {
			// 1. Extract top-level EPSG code directly via GeoKeys first to bypass component queries
			Integer epsgCode = extractEPSGCode(getFile(scope));
			Hints hints = new Hints();
			hints.put(Hints.SKIP_EXTERNAL_OVERVIEWS, Boolean.TRUE);

			if (epsgCode != null) {
				try {
					CoordinateReferenceSystem crs = CRS.decode("EPSG:" + epsgCode);
					hints.put(DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);
					return new GamaCRS(crs);
				} catch (Throwable ignored) {}
			}

			// 2. Fallback to standard reader with SKIP_EXTERNAL_OVERVIEWS hint
			try {
				final GeoTiffReader store = new GeoTiffReader(getFile(scope), hints);
				CoordinateReferenceSystem crs = store.getCoordinateReferenceSystem();
				store.dispose();
				return new GamaCRS(crs);
			} catch (final Throwable e) {
				// When the GeoTiFF reader (or one of its extensions) is not available
				// (e.g. imageio-ext missing on Android), silently fall back to the default
				// CRS instead of raising a spurious runtime error. Real read failures on
				// desktop are still reported.
				if (!isMissingClass(e)) {
					GAMA.reportError(scope,
							GamaRuntimeException.warning(
									"Problem when reading the CRS of the " + this.getOriginalPath() + " file", scope),
							false);
				}
			}
		}

		return null;
	}

	/**
	 * Extracts the top-level Projected or Geographic EPSG code from GeoTIFF GeoKeys metadata. Bypasses component-level
	 * database queries when running in database-free environments.
	 */
	/**
	 * Extracts the top-level Projected or Geographic EPSG code from GeoTIFF GeoKeys metadata. Bypasses component-level
	 * database queries when running in database-free environments.
	 */
	private Integer extractEPSGCode(final File file) {
		ImageInputStream in = null;
		ImageReader reader = null;
		try {
			in = ImageIO.createImageInputStream(file);
			if (in == null) return null;

			// Use the explicit GeoTools TIFF ImageReader SPI
			org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder metadata;
			it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi spi =
					new it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi();

			reader = spi.createReaderInstance();
			reader.setInput(in);

			javax.imageio.metadata.IIOMetadata iioMetadata = reader.getImageMetadata(0);
			metadata = new GeoTiffIIOMetadataDecoder(iioMetadata);

			if (metadata.hasGeoKey()) {
				// ProjectedCSTypeGeoKey = 1024
				String projStr = metadata.getGeoKey(1024);
				if (projStr != null) {
					try {
						int projectedCode = Integer.parseInt(projStr.trim());
						if (projectedCode != 32767 && projectedCode != 0) return projectedCode;
					} catch (Exception ignored) {}
				}

				// GeographicTypeGeoKey = 2048
				String geoStr = metadata.getGeoKey(2048);
				if (geoStr != null) {
					try {
						int geographicCode = Integer.parseInt(geoStr.trim());
						if (geographicCode != 32767 && geographicCode != 0) return geographicCode;
					} catch (Exception ignored) {}
				}
			}
		} catch (Throwable ignored) {} finally {
			if (reader != null) {
				try {
					reader.dispose();
				} catch (Exception ignored) {}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignored) {}
			}
		}
		return null;
	}

	@Override
	public void invalidateContents() {
		super.invalidateContents();
		if (coverage != null) { coverage.dispose(true); }
		coverage = null;
	}

	/**
	 * Value of.
	 *
	 * @param scope
	 *            the scope
	 * @param loc
	 *            the loc
	 * @return the double
	 */
	public Double valueOf(final IScope scope, final IPoint loc) {
		return valueOf(scope, loc.getX(), loc.getY());
	}

	/**
	 * Value of.
	 *
	 * @param scope
	 *            the scope
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @return the double
	 */
	public Double valueOf(final IScope scope, final double x, final double y) {
		if (getBuffer() == null) { fillBuffer(scope); }
		Object vals = null;
		try {
			vals = coverage.evaluate(new Position2D(x, y));
		} catch (final Exception e) {
			vals = noData.doubleValue();
		}
		final boolean doubleValues = vals instanceof double[];
		final boolean intValues = vals instanceof int[];
		final boolean byteValues = vals instanceof byte[];
		final boolean longValues = vals instanceof long[];
		final boolean floatValues = vals instanceof float[];
		Double val = null;
		if (doubleValues) {
			final double[] vd = (double[]) vals;
			val = vd[0];
		} else if (intValues) {
			final int[] vi = (int[]) vals;
			val = (double) vi[0];
		} else if (longValues) {
			final long[] vi = (long[]) vals;
			val = (double) vi[0];
		} else if (floatValues) {
			final float[] vi = (float[]) vals;
			val = (double) vi[0];
		} else if (byteValues) {
			final byte[] bv = (byte[]) vals;
			if (bv.length == 3) {
				final int red = bv[0] < 0 ? 256 + bv[0] : bv[0];
				final int green = bv[0] < 0 ? 256 + bv[1] : bv[1];
				final int blue = bv[0] < 0 ? 256 + bv[2] : bv[2];
				val = (red + green + blue) / 3.0;
			} else {
				val = (double) ((byte[]) vals)[0];
			}
		}
		return val;
	}

	@Override
	public int length(final IScope scope) {
		createCoverage(scope);
		return numRows * numCols;
	}

	@Override
	protected SimpleFeatureCollection getFeatureCollection(final IScope scope) {
		return null;
	}

	@Override
	public double getNoData(final IScope scope) {
		return noData == null ? IField.NO_NO_DATA : noData.doubleValue();
	}

	@Override
	public int getRows(final IScope scope) {
		createCoverage(scope);
		return numRows;
	}

	@Override
	public int getCols(final IScope scope) {
		createCoverage(scope);
		return numCols;
	}

	@Override
	public int getBandsNumber(final IScope scope) {
		createCoverage(scope);
		return nbBands;
	}

	@Override
	public double[] getBand(final IScope scope, final int index) {
		createCoverage(scope);
		read(scope, true, false);
		return Arrays.copyOf(records.bands.get(index), length(scope));
	}

	@Override
	protected IMatrix _matrixValue(final IScope scope, final IType contentsType, final IPoint preferredSize,
			final boolean copy) throws GamaRuntimeException {
		getContents(scope);
		return getField(scope);
	}

	@Override
	public void save(final IScope scope, final Facets parameters) {

	}

}
