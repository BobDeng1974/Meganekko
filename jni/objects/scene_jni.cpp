/* Copyright 2015 Samsung Electronics Co., LTD
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
 * JNI
 ***************************************************************************/

#include "scene.h"

#include "util/gvr_jni.h"

namespace mgn {
extern "C" {
JNIEXPORT jlong JNICALL
Java_com_eje_1c_meganekko_NativeScene_ctor(JNIEnv * env, jobject obj);

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_addSceneObject(JNIEnv * env,
        jobject obj, jlong jscene, jlong jscene_object);

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_removeSceneObject(JNIEnv * env,
        jobject obj, jlong jscene, jlong jscene_object);

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_setMainCamera(JNIEnv * env,
        jobject obj, jlong jscene, jlong jcamera);
JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_setFrustumCulling(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag);
JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_setOcclusionQuery(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag);

JNIEXPORT jlong JNICALL
Java_com_eje_1c_meganekko_NativeScene_ctor(JNIEnv * env, jobject obj) {
    return reinterpret_cast<jlong>(new Scene());
}

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_addSceneObject(JNIEnv * env,
        jobject obj, jlong jscene, jlong jscene_object) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    SceneObject* scene_object = reinterpret_cast<SceneObject*>(jscene_object);
    scene->addSceneObject(scene_object);
}

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_removeSceneObject(JNIEnv * env,
        jobject obj, jlong jscene, jlong jscene_object) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    SceneObject* scene_object = reinterpret_cast<SceneObject*>(jscene_object);
    scene->removeSceneObject(scene_object);
}

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_setMainCamera(JNIEnv * env,
        jobject obj, jlong jscene, jlong jcamera) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    Camera* camera = reinterpret_cast<Camera*>(jcamera);
    scene->set_main_camera(camera);
}

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_setFrustumCulling(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->set_frustum_culling(static_cast<bool>(flag));
}

JNIEXPORT void JNICALL
Java_com_eje_1c_meganekko_NativeScene_setOcclusionQuery(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->set_occlusion_culling(static_cast<bool>(flag));
}

}
}