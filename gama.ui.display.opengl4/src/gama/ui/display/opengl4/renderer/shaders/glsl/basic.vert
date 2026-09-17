#version 100

precision mediump float;

attribute vec3 aPos;
attribute vec4 aColor;
attribute vec2 aTexCoord;
attribute vec3 aNormal;

varying vec4 vertexColor;
varying vec2 TexCoord;
varying vec3 fragNormal;
varying vec3 fragPos;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMatrix;  // precomputed on CPU: mat3(transpose(inverse(model)))

void main()
{
    vec4 worldPos = model * vec4(aPos, 1.0);
    gl_Position = projection * view * worldPos;
    vertexColor = aColor;
    TexCoord = aTexCoord;
    fragNormal = normalize(normalMatrix * aNormal);
    fragPos = vec3(worldPos);
}
