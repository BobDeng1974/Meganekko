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

package com.eje_c.meganekko;

import com.eje_c.meganekko.utility.Threads;

import java.util.concurrent.Future;

/**
 * One of the key Meganekko classes: Encapsulates the data associated with rendering
 * a mesh.
 * This includes the {@link Mesh mesh} itself, the mesh's {@link Material
 * material}, camera association, rendering order, and various other parameters.
 */
public class RenderData extends Component {

    private static final String TAG = "Meganekko";
    private Mesh mMesh;
    private Material mMaterial;

    private static native void setMesh(long renderData, long mesh);

    private static native void setMaterial(long renderData, long material);

    private static native boolean isVisible(long renderData);

    private static native void setVisible(long renderData, boolean visible);

    private static native int getRenderingOrder(long renderData);

    private static native void setRenderingOrder(long renderData, int renderingOrder);

    private static native boolean getOffset(long renderData);

    private static native void setOffset(long renderData, boolean offset);

    private static native float getOffsetFactor(long renderData);

    private static native void setOffsetFactor(long renderData, float offsetFactor);

    private static native float getOffsetUnits(long renderData);

    private static native void setOffsetUnits(long renderData, float offsetUnits);

    private static native boolean getDepthTest(long renderData);

    private static native void setDepthTest(long renderData, boolean depthTest);

    private static native boolean getAlphaBlend(long renderData);

    private static native void setAlphaBlend(long renderData, boolean alphaBlend);

    private static native float getOpacity(long renderData);

    private static native void setOpacity(long renderData, float opacity);

    @Override
    protected native long initNativeInstance();

    /**
     * @return The {@link Mesh mesh} being rendered.
     */
    public Mesh getMesh() {
        return mMesh;
    }

    /**
     * Asynchronously set the {@link Mesh mesh} to be rendered.
     * Uses a background thread from the thread pool to wait for the
     * {@code Future.get()} method; unless you are loading dozens of meshes
     * asynchronously, the extra overhead should be modest compared to the cost
     * of loading a mesh.
     *
     * @param mesh The mesh to be rendered.
     */
    public void setMesh(final Future<Mesh> mesh) {
        Threads.spawn(new Runnable() {

            @Override
            public void run() {
                try {
                    setMesh(mesh.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Set the {@link Mesh mesh} to be rendered.
     *
     * @param mesh The mesh to be rendered.
     */
    public void setMesh(Mesh mesh) {
        synchronized (this) {
            mMesh = mesh;
        }
        setMesh(getNative(), mesh.getNative());
    }

    /**
     * @return The {@link Material material} the {@link Mesh mesh} is
     * being rendered with.
     */
    public Material getMaterial() {
        return mMaterial;
    }

    /**
     * Set the {@link Material material} the mesh will be rendered with.
     *
     * @param material The {@link Material material} for rendering.
     */
    public void setMaterial(Material material) {
        this.mMaterial = material;
        setMaterial(getNative(), material.getNative());
    }

    public boolean isVisible() {
        return isVisible(getNative());
    }

    public void setVisible(boolean visible) {
        setVisible(getNative(), visible);
    }

    /**
     * @return The order in which this mesh will be rendered.
     * @see RenderingOrder
     */
    public int getRenderingOrder() {
        return getRenderingOrder(getNative());
    }

    /**
     * Set the order in which this mesh will be rendered.
     *
     * @param renderingOrder See {@link RenderingOrder}
     */
    public void setRenderingOrder(int renderingOrder) {
        setRenderingOrder(getNative(), renderingOrder);
    }

    /**
     * @return {@code true} if {@code GL_POLYGON_OFFSET_FILL} is enabled,
     * {@code false} if not.
     */
    public boolean getOffset() {
        return getOffset(getNative());
    }

    /**
     * Set the {@code GL_POLYGON_OFFSET_FILL} option
     *
     * @param offset {@code true} if {@code GL_POLYGON_OFFSET_FILL} should be
     *               enabled, {@code false} if not.
     */
    public void setOffset(boolean offset) {
        setOffset(getNative(), offset);
    }

    /**
     * @return The {@code factor} value passed to {@code glPolygonOffset()} if
     * {@code GL_POLYGON_OFFSET_FILL} is enabled.
     * @see #setOffset(boolean)
     */
    public float getOffsetFactor() {
        return getOffsetFactor(getNative());
    }

    /**
     * Set the {@code factor} value passed to {@code glPolygonOffset()} if
     * {@code GL_POLYGON_OFFSET_FILL} is enabled.
     *
     * @param offsetFactor Per OpenGL docs: Specifies a scale factor that is used to
     *                     create a variable depth offset for each polygon. The initial
     *                     value is 0.
     * @see #setOffset(boolean)
     */
    public void setOffsetFactor(float offsetFactor) {
        setOffsetFactor(getNative(), offsetFactor);
    }

    /**
     * @return The {@code units} value passed to {@code glPolygonOffset()} if
     * {@code GL_POLYGON_OFFSET_FILL} is enabled.
     * @see #setOffset(boolean)
     */
    public float getOffsetUnits() {
        return getOffsetUnits(getNative());
    }

    /**
     * Set the {@code units} value passed to {@code glPolygonOffset()} if
     * {@code GL_POLYGON_OFFSET_FILL} is enabled.
     *
     * @param offsetUnits Per OpenGL docs: Is multiplied by an implementation-specific
     *                    value to create a constant depth offset. The initial value is
     *                    0.
     * @see #setOffset(boolean)
     */
    public void setOffsetUnits(float offsetUnits) {
        setOffsetUnits(getNative(), offsetUnits);
    }

    /**
     * @return {@code true} if {@code GL_DEPTH_TEST} is enabled, {@code false}
     * if not.
     */
    public boolean getDepthTest() {
        return getDepthTest(getNative());
    }

    /**
     * Set the {@code GL_DEPTH_TEST} option
     *
     * @param depthTest {@code true} if {@code GL_DEPTH_TEST} should be enabled,
     *                  {@code false} if not.
     */
    public void setDepthTest(boolean depthTest) {
        setDepthTest(getNative(), depthTest);
    }

    /**
     * @return {@code true} if {@code GL_BLEND} is enabled, {@code false} if
     * not.
     */
    public boolean getAlphaBlend() {
        return getAlphaBlend(getNative());
    }

    /**
     * Set the {@code GL_BLEND} option
     *
     * @param alphaBlend {@code true} if {@code GL_BLEND} should be enabled,
     *                   {@code false} if not.
     */
    public void setAlphaBlend(boolean alphaBlend) {
        setAlphaBlend(getNative(), alphaBlend);
    }

    public void setOpacity(float opacity) {
        setOpacity(getNative(), opacity);
    }

    public float getOpacity() {
        return getOpacity(getNative());
    }

    /**
     * Rendering hints.
     * You might expect the rendering process to sort the scene graph, from back
     * to front, so it can then draw translucent objects over the objects behind
     * them. But that's not how Meganekko works. Instead, it sorts the scene graph by
     * render order, then draws the sorted graph in traversal order. (Please
     * don't waste your time getting angry or trying to make sense of this;
     * please just take it as a bald statement of How Meganekko Currently Works.)
     * The point is, to get transparency to work as you expect, you do need to
     * explicitly call {@link RenderData#setRenderingOrder(int)
     * setRenderingOrder():} objects are sorted from low render order to high
     * render order, so that a {@link #GEOMETRY} object will show through a
     * {@link #TRANSPARENT} object.
     */
    public abstract static class RenderingOrder {
        /**
         * Rendered first, below any other objects at the same distance from the
         * camera
         */
        public static final int BACKGROUND = 1000;
        /**
         * The default render order, if you don't explicitly call
         * {@link RenderData#setRenderingOrder(int)}
         */
        public static final int GEOMETRY = 2000;
        /**
         * The rendering order for see-through objects
         */
        public static final int TRANSPARENT = 3000;
        /**
         * The rendering order for sprites {@literal &c.}
         */
        public static final int OVERLAY = 4000;
    }
}
