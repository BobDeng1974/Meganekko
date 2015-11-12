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

package com.eje_c.meganekko;

/**
 * Wrapper for a {@code GL_TEXTURE_EXTERNAL_OES} texture. This is typically used
 * to work with textures that are backed by cameras or video buffers.
 */
public class ExternalTexture extends Texture {
    /**
     * @param vrContext Current vrContext
     */
    public ExternalTexture(VrContext vrContext) {
        super(vrContext);
    }

    @Override
    protected native long initNativeInstance();
}
