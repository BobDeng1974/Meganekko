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
 * Renders a cube map texture in reflection mode without light.
 ***************************************************************************/

#include "cubemap_reflection_shader.h"

#include "objects/material.h"
#include "objects/mesh.h"
#include "objects/components/render_data.h"
#include "objects/textures/texture.h"
#include "util/gvr_gl.h"

// OpenGL Cube map texture uses coordinate system different to other OpenGL functions:
// Positive x pointing right, positive y pointing up, positive z pointing inward.
// It is a left-handed system, while other OpenGL functions use right-handed system.
// The side faces are also oriented up-side down as illustrated below.
//
// Since the origin of Android bitmap is at top left, and the origin of OpenGL texture
// is at bottom left, when we use Android bitmap to create OpenGL texture, it is already
// up-side down. So we do not need to flip them again.
//
// We do need to flip the z-coordinate to be consistent with the left-handed system.
//    _________
//   /        /|
//  /________/ |
//  |        | |    +y
//  |        | |    |  +z
//  |        | /    | /
//  |________|/     |/___ +x
//
//  Positive x    Positive y    Positive z
//      ______        ______        ______
//     |      |      |      |      |      |
//  -y |      |   +z |      |   -y |      |
//  |  |______|   |  |______|   |  |______|
//  |___ -z       |___ +x       |___ +x
//
//  Negative x    Negative y    Negative z
//      ______        ______        ______
//     |      |      |      |      |      |
//  -y |      |   -z |      |   -y |      |
//  |  |______|   |  |______|   |  |______|
//  |___ +z       |___ +x       |___ -x
//
// (http://www.nvidia.com/object/cube_map_ogl_tutorial.html)
// (http://stackoverflow.com/questions/11685608/convention-of-faces-in-opengl-cubemapping)

namespace mgn {
static const char VERTEX_SHADER[] =
        "attribute vec4 a_position;\n"
                "attribute vec3 a_normal;\n"
                "uniform mat4 u_mv;\n"
                "uniform mat4 u_mv_it;\n"
                "uniform mat4 u_mvp;\n"
                "varying vec3 v_viewspace_position;\n"
                "varying vec3 v_viewspace_normal;\n"
                "void main() {\n"
                "  vec4 v_viewspace_position_vec4 = u_mv * a_position;\n"
                "  v_viewspace_position = v_viewspace_position_vec4.xyz / v_viewspace_position_vec4.w;\n"
                "  v_viewspace_normal = (u_mv_it * vec4(a_normal, 1.0)).xyz;\n"
                "  gl_Position = u_mvp * a_position;\n"
                "}\n";

static const char FRAGMENT_SHADER[] =
        "precision highp float;\n"
                "uniform samplerCube u_texture;\n"
                "uniform vec3 u_color;\n"
                "uniform float u_opacity;\n"
                "uniform mat4 u_view_i;\n"
                "varying vec3 v_viewspace_position;\n"
                "varying vec3 v_viewspace_normal;\n"
                "void main()\n"
                "{\n"
                "  vec3 v_reflected_position = reflect(v_viewspace_position, normalize(v_viewspace_normal));\n"
                "  vec3 v_tex_coord = (u_view_i * vec4(v_reflected_position, 1.0)).xyz;\n"
                "  v_tex_coord.z = -v_tex_coord.z;\n"
                "  vec4 color = textureCube(u_texture, v_tex_coord.xyz);\n"
                "  gl_FragColor = vec4(color.r * u_color.r * u_opacity, color.g * u_color.g * u_opacity, color.b * u_color.b * u_opacity, color.a * u_opacity);\n"
                "}\n";

CubemapReflectionShader::CubemapReflectionShader() :
        a_position_(0), a_normal_(0), u_mv_(0), u_mv_it_(0), u_mvp_(
                0), u_view_i_(0), u_texture_(0), u_color_(0), u_opacity_(0) {
    program_ = BuildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    a_position_ = glGetAttribLocation(program_.program, "a_position");
    a_normal_ = glGetAttribLocation(program_.program, "a_normal");
    u_mv_ = glGetUniformLocation(program_.program, "u_mv");
    u_mv_it_ = glGetUniformLocation(program_.program, "u_mv_it");
    u_mvp_ = glGetUniformLocation(program_.program, "u_mvp");
    u_view_i_ = glGetUniformLocation(program_.program, "u_view_i");
    u_texture_ = glGetUniformLocation(program_.program, "u_texture");
    u_color_ = glGetUniformLocation(program_.program, "u_color");
    u_opacity_ = glGetUniformLocation(program_.program, "u_opacity");
}

CubemapReflectionShader::~CubemapReflectionShader() {
    recycle();
}

void CubemapReflectionShader::recycle() {
    DeleteProgram(program_);
}

void CubemapReflectionShader::render(const OVR::Matrix4f& mv_matrix,
        const OVR::Matrix4f& mv_it_matrix, const OVR::Matrix4f& view_invers_matrix,
        const OVR::Matrix4f& mvp_matrix, RenderData* render_data, Material* material) {
    Mesh* mesh = render_data->mesh();
    Texture* texture = material->getTexture("main_texture");
    OVR::Vector3f color = material->getVec3("color");
    float opacity = material->getFloat("opacity");

    if (texture->getTarget() != GL_TEXTURE_CUBE_MAP) {
        std::string error =
                "CubemapReflectionShader::render : texture with wrong target";
        throw error;
    }

    mesh->setVertexLoc(a_position_);
    mesh->setNormalLoc(a_normal_);
    mesh->generateVAO(Material::CUBEMAP_REFLECTION_SHADER);

    glUseProgram(program_.program);

    glUniformMatrix4fv(u_mv_, 1, GL_TRUE, mv_matrix.M[0]);
    glUniformMatrix4fv(u_mv_it_, 1, GL_TRUE, mv_it_matrix.M[0]);
    glUniformMatrix4fv(u_mvp_, 1, GL_TRUE, mvp_matrix.M[0]);
    glUniformMatrix4fv(u_view_i_, 1, GL_TRUE, view_invers_matrix.M[0]);
    glActiveTexture (GL_TEXTURE0);
    glBindTexture(texture->getTarget(), texture->getId());
    glUniform1i(u_texture_, 0);
    glUniform3f(u_color_, color.x, color.y, color.z);
    glUniform1f(u_opacity_, opacity);

    glBindVertexArray(mesh->getVAOId(Material::CUBEMAP_REFLECTION_SHADER));
    glDrawElements(GL_TRIANGLES, mesh->triangles().size(), GL_UNSIGNED_SHORT,
            0);
    glBindVertexArray(0);

    checkGlError("CubemapReflectionShader::render");
}

}
;
