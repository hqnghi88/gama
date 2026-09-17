#version 100

precision mediump float;

varying vec4 vertexColor;
varying vec2 TexCoord;
varying vec3 fragNormal;
varying vec3 fragPos;

uniform sampler2D texture1;
uniform int useTexture;      // 0 or 1 (bool not available in GLSL ES 1.00)

// Lighting uniforms
uniform int   useLighting;   // 0 or 1
uniform vec3  ambientColor;
uniform vec3  lightPosition;
uniform vec3  lightColor;
uniform vec3  viewPos;
uniform float shininess;

void main()
{
    vec4 baseColor;
    if (useTexture != 0) {
        baseColor = texture2D(texture1, TexCoord) * vertexColor;
    } else {
        baseColor = vertexColor;
    }

    if (useLighting == 0) {
        gl_FragColor = baseColor;
        return;
    }

    // --- Phong lighting ---
    vec3 norm    = normalize(fragNormal);
    vec3 lightDir = normalize(lightPosition - fragPos);

    // Ambient
    vec3 ambient = ambientColor * baseColor.rgb;

    // Diffuse
    float diff   = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor * baseColor.rgb;

    // Specular (Blinn-Phong half-vector)
    vec3 viewDir    = normalize(viewPos - fragPos);
    vec3 halfDir    = normalize(lightDir + viewDir);
    float spec      = pow(max(dot(norm, halfDir), 0.0), max(shininess, 1.0));
    vec3 specular   = spec * lightColor * 0.3;

    vec3 result = ambient + diffuse + specular;
    gl_FragColor = vec4(result, baseColor.a);
}
