/*
 * Copyright 2015 eje inc.
 * Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************
 * Renders a GL_TEXTURE_EXTERNAL_OES texture.
 ***************************************************************************/

#include "oes_shader.h"

#include "objects/material.h"
#include "objects/mesh.h"
#include "objects/components/render_data.h"
#include "util/gvr_gl.h"

namespace mgn {

static const char VERTEX_SHADER[] =
        "attribute vec4 Position;\n"
        "attribute vec2 TexCoord;\n"
        "uniform mat4 Mvpm;\n"
        "varying highp vec2 oTexCoord;\n"
        "void main() {\n"
        "  oTexCoord = TexCoord;\n"
        "  gl_Position = Mvpm * Position;\n"
        "}\n";

static const char FRAGMENT_SHADER[] =
        "#extension GL_OES_EGL_image_external : require\n"
        "precision highp float;\n"
        "uniform samplerExternalOES Texture0;\n"
        "uniform vec3 UniformColor;\n"
        "uniform float Opacity;\n"
        "varying highp vec2 oTexCoord;\n"
        "void main() {\n"
        "  vec4 color = texture2D(Texture0, oTexCoord);"
        "  gl_FragColor = vec4(color.r * UniformColor.r * Opacity, color.g * UniformColor.g * Opacity, color.b * UniformColor.b * Opacity, color.a * Opacity);\n"
        "}\n";

OESShader::OESShader() {
    program = BuildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    opacity = glGetUniformLocation(program.program, "Opacity");
}

OESShader::~OESShader() {
    recycle();
}

void OESShader::recycle() {
    DeleteProgram(program);
}

void OESShader::render(const Matrix4f & mvpMatrix, RenderData * renderData, Material * material) {
    
    Mesh * mesh = renderData->mesh();
    Texture * mainTexture = material->getTexture("main_texture");
    Vector3f color = material->getVec3("color");

    if (mainTexture->getTarget() != GL_TEXTURE_EXTERNAL_OES) {
        std::string error = "OESShader::render : texture with wrong target";
        throw error;
    }

    mesh->setVertexLoc(VERTEX_ATTRIBUTE_LOCATION_POSITION);
    mesh->setTexCoordLoc(VERTEX_ATTRIBUTE_LOCATION_UV0);
    mesh->generateVAO(Material::OES_SHADER);

    glUseProgram(program.program);

    glUniformMatrix4fv(program.uMvp, 1, GL_TRUE, mvpMatrix.M[0]);
    glActiveTexture (GL_TEXTURE0);
    glBindTexture(mainTexture->getTarget(), mainTexture->getId());
    glUniform3f(program.uColor, color.x, color.y, color.z);
    glUniform1f(opacity, material->getFloat("opacity"));

    glBindVertexArray(mesh->getVAOId(Material::OES_SHADER));
    glDrawElements(GL_TRIANGLES, mesh->triangles().size(), GL_UNSIGNED_SHORT, 0);
    glBindVertexArray(0);

    glActiveTexture( GL_TEXTURE0 );
    glBindTexture( GL_TEXTURE_EXTERNAL_OES, 0 );
    
    GL_CheckErrors("OESShader::render");
}

}
;
