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

package com.eje_c.meganekko.asynchronous;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.eje_c.meganekko.*;
import com.eje_c.meganekko.AndroidResource.BitmapTextureCallback;
import com.eje_c.meganekko.AndroidResource.CancelableCallback;
import com.eje_c.meganekko.AndroidResource.CompressedTextureCallback;
import com.eje_c.meganekko.utility.Log;
import com.eje_c.meganekko.utility.ResourceCache;
import com.eje_c.meganekko.utility.Threads;

import android.graphics.Bitmap;

/**
 * Internal API for asynchronous resource loading.
 * 
 * You will normally call into this class through
 * {@link GLContext#loadCompressedTexture(AndroidResource.CompressedTextureCallback, AndroidResource)}
 * or
 * {@link GLContext#loadBitmapTexture(AndroidResource.BitmapTextureCallback, AndroidResource)}
 * or
 * {@link GLContext#loadMesh(AndroidResource.MeshCallback, AndroidResource)}
 * .
 */
public class AsynchronousResourceLoader {

    /**
     * Get device parameters and so on.
     * 
     * This is an internal method, public only so it can be called across
     * package boundaries. Calling it from user code is both harmless and
     * pointless.
     */
    public static void setup(GLContext gvrContext) {
        AsyncBitmapTexture.setup(gvrContext);
    }

    /**
     * Load a compressed texture asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadCompressedTexture(AndroidResource.CompressedTextureCallback, AndroidResource)}
     * : it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Stream containing a compressed texture
     * @throws IllegalArgumentException
     *             If {@code gvrContext} or {@code callback} parameters are
     *             {@code null}
     */
    public static void loadCompressedTexture(final GLContext gvrContext,
            ResourceCache<Texture> textureCache,
            final CompressedTextureCallback callback,
            final AndroidResource resource) throws IllegalArgumentException {
        loadCompressedTexture(gvrContext, textureCache, callback, resource,
                GVRCompressedTexture.DEFAULT_QUALITY);
    }

    /**
     * Load a compressed texture asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadCompressedTexture(AndroidResource.CompressedTextureCallback, AndroidResource)}
     * : it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link AndroidResource} parameter eliminates six overloads.
     * @param quality
     *            Speed/quality tradeoff: should be one of
     *            {@link GVRCompressedTexture#SPEED},
     *            {@link GVRCompressedTexture#BALANCED}, or
     *            {@link GVRCompressedTexture#QUALITY}, but other values are
     *            'clamped' to one of the recognized values.
     * @throws IllegalArgumentException
     *             If {@code gvrContext} or {@code callback} parameters are
     *             {@code null}
     */
    public static void loadCompressedTexture(final GLContext gvrContext,
            final ResourceCache<Texture> textureCache,
            final CompressedTextureCallback callback,
            final AndroidResource resource, final int quality)
            throws IllegalArgumentException {
        validateCallbackParameters(gvrContext, callback, resource);

        final Texture cached = textureCache == null ? null : textureCache
                .get(resource);
        if (cached != null) {
            gvrContext.runOnGlThread(new Runnable() {

                @Override
                public void run() {
                    callback.loaded(cached, resource);
                }
            });
        } else {
            // Load the bytes on a background thread
            Threads.spawn(new Runnable() {

                @Override
                public void run() {
                    try {
                        final CompressedTexture compressedTexture = CompressedTexture
                                .load(resource.getStream(), false);
                        resource.closeStream();
                        // Create texture on GL thread
                        gvrContext.runOnGlThread(new Runnable() {

                            @Override
                            public void run() {
                                Texture texture = compressedTexture
                                        .toTexture(gvrContext, quality);
                                if (textureCache != null) {
                                    textureCache.put(resource, texture);
                                }
                                callback.loaded(texture, resource);
                            }
                        });
                    } catch (Exception e) {
                        callback.failed(e, resource);
                    }
                }
            });
        }
    }

    /**
     * Load a bitmap texture asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadBitmapTexture(AndroidResource.BitmapTextureCallback, AndroidResource, int)}
     * - it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link AndroidResource} parameter eliminates six overloads.
     * @param priority
     *            A value {@literal >=} {@link GLContext#LOWEST_PRIORITY} and
     *            {@literal <=} {@link GLContext#HIGHEST_PRIORITY}
     * @throws IllegalArgumentException
     *             If {@code priority} {@literal <}
     *             {@link GLContext#LOWEST_PRIORITY} or {@literal >}
     *             {@link GLContext#HIGHEST_PRIORITY}, or any of the other
     *             parameters are {@code null}.
     */
    public static void loadBitmapTexture(GLContext gvrContext,
            ResourceCache<Texture> textureCache,
            final BitmapTextureCallback callback,
            final AndroidResource resource, int priority)
            throws IllegalArgumentException {
        validatePriorityCallbackParameters(gvrContext, callback, resource,
                priority);

        final Texture cached = textureCache == null ? null : textureCache
                .get(resource);
        if (cached != null) {
            gvrContext.runOnGlThread(new Runnable() {

                @Override
                public void run() {
                    callback.loaded(cached, resource);
                }
            });
        } else {
            BitmapTextureCallback actualCallback = textureCache == null ? callback
                    : ResourceCache.wrapCallback(textureCache, callback);
            AsyncBitmapTexture.loadTexture(gvrContext, actualCallback,
                    resource, priority);
        }
    }

    /**
     * Load a (compressed or bitmapped) texture asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadTexture(com.eje_c.meganekko.AndroidResource.TextureCallback, AndroidResource, int, int)}
     * - it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link AndroidResource} parameter eliminates six overloads.
     * @param priority
     *            A value {@literal >=} {@link GLContext#LOWEST_PRIORITY} and
     *            {@literal <=} {@link GLContext#HIGHEST_PRIORITY}
     * @throws IllegalArgumentException
     *             If {@code priority} {@literal <}
     *             {@link GLContext#LOWEST_PRIORITY} or {@literal >}
     *             {@link GLContext#HIGHEST_PRIORITY}, or any of the other
     *             parameters are {@code null}.
     */
    public static void loadTexture(final GLContext gvrContext,
            final ResourceCache<Texture> textureCache,
            final CancelableCallback<Texture> callback,
            final AndroidResource resource, final int priority,
            final int quality) {
        validateCallbackParameters(gvrContext, callback, resource);

        final Texture cached = textureCache == null ? null : textureCache
                .get(resource);
        if (cached != null) {
            gvrContext.runOnGlThread(new Runnable() {

                @Override
                public void run() {
                    callback.loaded(cached, resource);
                }
            });
        } else {
            // 'Sniff' out compressed textures on a thread from the thread-pool
            Threads.spawn(new Runnable() {

                @Override
                public void run() {
                    try {
                        // Save stream position
                        resource.mark();

                        CompressedTextureLoader loader;
                        try {
                            loader = CompressedTexture.sniff(resource
                                    .getStream());
                        } finally {
                            resource.reset();
                        }

                        if (loader != null) {
                            // We have a compressed texture: proceed on this
                            // thread
                            final CompressedTexture compressedTexture = CompressedTexture
                                    .parse(resource.getStream(), false, loader);
                            resource.closeStream();

                            // Create texture on GL thread
                            gvrContext.runOnGlThread(new Runnable() {

                                @Override
                                public void run() {
                                    Texture texture = compressedTexture
                                            .toTexture(gvrContext, quality);
                                    textureCache.put(resource, texture);
                                    callback.loaded(texture, resource);
                                }
                            });
                        } else {
                            // We don't have a compressed texture: pass to
                            // AsyncBitmapTexture code
                            CancelableCallback<Texture> actualCallback = textureCache == null ? callback
                                    : textureCache.wrapCallback(callback);
                            AsyncBitmapTexture.loadTexture(gvrContext,
                                    actualCallback, resource, priority);
                        }
                    } catch (Exception e) {
                        callback.failed(e, resource);
                    }
                }
            });
        }
    }

    /**
     * Load a (compressed or bitmapped) texture asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadFutureTexture(AndroidResource, int, int)} - it
     * will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link AndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * @param quality
     *            The compressed texture {@link GVRCompressedTexture#mQuality
     *            quality} parameter: should be one of
     *            {@link GVRCompressedTexture#SPEED},
     *            {@link GVRCompressedTexture#BALANCED}, or
     *            {@link GVRCompressedTexture#QUALITY}, but other values are
     *            'clamped' to one of the recognized values. Please note that
     *            this (currently) only applies to compressed textures; normal
     *            {@linkplain BitmapTexture bitmapped textures} don't take a
     *            quality parameter.
     * @return A {@link Future} that you can pass to methods like
     *         {@link Shaders#setMainTexture(Future)}
     */
    public static Future<Texture> loadFutureTexture(GLContext gvrContext,
            ResourceCache<Texture> textureCache,
            AndroidResource resource, int priority, int quality) {
        Texture cached = textureCache == null ? null : textureCache
                .get(resource);
        if (cached != null) {
            return new FutureWrapper<Texture>(cached);
        } else {
            FutureResource<Texture> result = new FutureResource<Texture>();

            loadTexture(gvrContext, textureCache, result.callback, resource,
                    priority, quality);

            return result;
        }
    }

    /**
     * Load a cube map texture asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadFutureCubemapTexture(AndroidResource)} - it will
     * usually be more convenient (and more efficient) to call that directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param resource
     *            A steam containing a zip file which contains six bitmaps. The
     *            six bitmaps correspond to +x, -x, +y, -y, +z, and -z faces of
     *            the cube map texture respectively. The default names of the
     *            six images are "posx.png", "negx.png", "posy.png", "negx.png",
     *            "posz.png", and "negz.png", which can be changed by calling
     *            {@link CubemapTexture#setFaceNames(String[])}.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * @return A {@link Future} that you can pass to methods like
     *         {@link Shaders#setMainTexture(Future)}
     */
    public static Future<Texture> loadFutureCubemapTexture(
            GLContext gvrContext, ResourceCache<Texture> textureCache,
            AndroidResource resource, int priority,
            Map<String, Integer> faceIndexMap) {
        Texture cached = textureCache.get(resource);
        if (cached != null) {
            return new FutureWrapper<Texture>(cached);
        } else {
            FutureResource<Texture> result = new FutureResource<Texture>();

            AsyncCubemapTexture.loadTexture(gvrContext, result.callback,
                    resource, priority, faceIndexMap);

            return result;
        }
    }

    /**
     * 
     * Load a GL mesh asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadMesh(AndroidResource.MeshCallback, AndroidResource, int)}
     * - it will usually be more convenient to call that directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a 3D model. Taking a
     *            {@link AndroidResource} parameter eliminates six overloads.
     * @param priority
     *            A value {@literal >=} {@link GLContext#LOWEST_PRIORITY} and
     *            {@literal <=} {@link GLContext#HIGHEST_PRIORITY}
     * @throws IllegalArgumentException
     *             If {@code priority} {@literal <}
     *             {@link GLContext#LOWEST_PRIORITY} or {@literal >}
     *             {@link GLContext#HIGHEST_PRIORITY}, or any of the other
     *             parameters are {@code null}.
     */
    // This method does not take a ResourceCache<GVRMeh> parameter because it
    // (indirectly) calls GVRContext.loadMesh() which 'knows about' the cache
    public static void loadMesh(GLContext gvrContext,
            CancelableCallback<Mesh> callback, AndroidResource resource,
            int priority) {
        validatePriorityCallbackParameters(gvrContext, callback, resource,
                priority);

        AsyncMesh.loadMesh(gvrContext, callback, resource, priority);
    }

    /**
     * Load a GL mesh asynchronously.
     * 
     * This is the implementation of
     * {@link GLContext#loadFutureMesh(AndroidResource, int)} - it will
     * usually be more convenient to call that directly.
     * 
     * @param gvrContext
     *            The GVRF context
     * @param resource
     *            Basically, a stream containing a 3D model. The
     *            {@link AndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * @return A {@link Future} that you can pass to
     *         {@link RenderData#setMesh(Future)}
     */
    public static Future<Mesh> loadFutureMesh(GLContext gvrContext,
            AndroidResource resource, int priority) {
        FutureResource<Mesh> result = new FutureResource<Mesh>();

        loadMesh(gvrContext, result.callback, resource, priority);

        return result;
    }

    private static class FutureResource<T extends HybridObject> implements
            Future<T> {

        private static final String TAG = Log.tag(FutureResource.class);

        /** Do all our synchronization on data private to this instance */
        private final Object[] lock = new Object[0];

        private T result = null;
        private Throwable error = null;
        private boolean pending = true;
        private boolean canceled = false;

        private final CancelableCallback<T> callback = new CancelableCallback<T>() {

            @Override
            public void loaded(T data, AndroidResource androidResource) {
                synchronized (lock) {
                    result = data;
                    pending = false;
                    lock.notifyAll();
                }
            }

            @Override
            public void failed(Throwable t, AndroidResource androidResource) {
                Log.d(TAG, "failed(%s), %s", androidResource, t);
                synchronized (lock) {
                    error = t;
                    pending = false;
                    lock.notifyAll();
                }
            }

            @Override
            public boolean stillWanted(AndroidResource androidResource) {
                return canceled == false;
            }
        };

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            canceled = true;
            return pending;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return get(0);
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return get(unit.toMillis(timeout));
        }

        private T get(long millis) throws InterruptedException,
                ExecutionException {
            synchronized (lock) {
                if (pending) {
                    lock.wait(millis);
                }
            }
            if (canceled) {
                throw new CancellationException();
            }
            if (error != null) {
                throw new ExecutionException(error);
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            return canceled;
        }

        @Override
        public boolean isDone() {
            return pending == false;
        }

    }

    private static <T extends HybridObject> void validateCallbackParameters(
            GLContext gvrContext, AndroidResource.Callback<T> callback,
            AndroidResource resource) {
        if (gvrContext == null) {
            throw new IllegalArgumentException("gvrContext == null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback == null");
        }
        if (resource == null) {
            throw new IllegalArgumentException("resource == null");
        }
    }

    private static <T extends HybridObject> void validatePriorityCallbackParameters(
            GLContext gvrContext, AndroidResource.Callback<T> callback,
            AndroidResource resource, int priority) {
        validateCallbackParameters(gvrContext, callback, resource);
        if (priority < GLContext.LOWEST_PRIORITY
                || priority > GLContext.HIGHEST_PRIORITY) {
            throw new IllegalArgumentException(
                    "Priority < GVRContext.LOWEST_PRIORITY or > GVRContext.HIGHEST_PRIORITY");
        }
    }

    /**
     * An internal method, public only so that GVRContext can make cross-package
     * calls.
     * 
     * A synchronous (blocking) wrapper around
     * {@link android.graphics.BitmapFactory#decodeStream(InputStream)
     * BitmapFactory.decodeStream} that uses an
     * {@link android.graphics.BitmapFactory.Options} <code>inTempStorage</code>
     * decode buffer. On low memory, returns half (quarter, eighth, ...) size
     * images.
     * <p>
     * If {@code stream} is a {@link FileInputStream} and is at offset 0 (zero),
     * uses
     * {@link android.graphics.BitmapFactory#decodeFileDescriptor(FileDescriptor)
     * BitmapFactory.decodeFileDescriptor()} instead of
     * {@link android.graphics.BitmapFactory#decodeStream(InputStream)
     * BitmapFactory.decodeStream()}.
     * 
     * @param stream
     *            Bitmap stream
     * @param closeStream
     *            If {@code true}, closes {@code stream}
     * @return Bitmap, or null if cannot be decoded into a bitmap
     */
    public static Bitmap decodeStream(InputStream stream, boolean closeStream) {
        return AsyncBitmapTexture.decodeStream(stream,
                AsyncBitmapTexture.glMaxTextureSize,
                AsyncBitmapTexture.glMaxTextureSize, true, null, closeStream);
    }
}