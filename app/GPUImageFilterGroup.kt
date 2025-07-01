package me.devsaki.hentoid.gles_renderer.filter

import android.annotation.SuppressLint
import android.opengl.GLES20
import me.devsaki.hentoid.gles_renderer.CUBE
import me.devsaki.hentoid.gles_renderer.util.Rotation
import me.devsaki.hentoid.gles_renderer.util.TEXTURE_NO_ROTATION
import me.devsaki.hentoid.gles_renderer.util.getRotation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Resembles a filter that consists of multiple filters applied after each other.
 */
open class GPUImageFilterGroup(private val filters: MutableList<GPUImageFilter>) :
    GPUImageFilter() {

    private var mergedFilters: MutableList<GPUImageFilter> = ArrayList()
    private var frameBuffers: IntArray? = null
    private var frameBufferTextures: IntArray? = null

    private val glCubeBuffer: FloatBuffer
    private val glTextureBuffer: FloatBuffer
    private val glTextureFlipBuffer: FloatBuffer

    init {
        updateMergedFilters()
        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        glCubeBuffer.put(CUBE).position(0)
        glTextureBuffer =
            ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        glTextureBuffer.put(TEXTURE_NO_ROTATION).position(0)
        val flipTexture: FloatArray =
            getRotation(Rotation.NORMAL, flipHorizontal = false, flipVertical = true)
        glTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        glTextureFlipBuffer.put(flipTexture).position(0)
    }

    fun addFilter(aFilter: GPUImageFilter) {
        filters.add(aFilter)
        updateMergedFilters()
    }

    override fun onInit() {
        super.onInit()
        for (filter in filters) {
            filter.ifNeedInit()
        }
    }

    override fun onDestroy() {
        destroyFramebuffers()
        for (filter in filters) {
            filter.destroy()
        }
        super.onDestroy()
    }

    private fun destroyFramebuffers() {
        if (frameBufferTextures != null) {
            GLES20.glDeleteTextures(frameBufferTextures!!.size, frameBufferTextures, 0)
            frameBufferTextures = null
        }
        if (frameBuffers != null) {
            GLES20.glDeleteFramebuffers(frameBuffers!!.size, frameBuffers, 0)
            frameBuffers = null
        }
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        destroyFramebuffers()
        var size = filters.size
        for (i in 0 until size) {
            filters[i].onOutputSizeChanged(width, height)
        }
        if (mergedFilters.size > 0) {
            size = mergedFilters.size
            frameBuffers = IntArray(size - 1)
            frameBufferTextures = IntArray(size - 1)
            for (i in 0 until size - 1) {
                GLES20.glGenFramebuffers(1, frameBuffers, i)
                GLES20.glGenTextures(1, frameBufferTextures, i)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures!![i])
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                GLES20.glFramebufferTexture2D(
                    GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, frameBufferTextures!![i], 0
                )
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            }
        }
    }

    fun onOutputSizeChangedAfter(width: Int, height: Int, index: Int) {
        super.onOutputSizeChanged(width, height)
        var size = filters.size
        for (i in index until size) {
            filters[i].onOutputSizeChanged(width, height)
        }
        if (mergedFilters.size > index) {

            GLES20.glViewport(0, 0, width, height)

            size = mergedFilters.size
            frameBuffers = IntArray(size - 1)
            frameBufferTextures = IntArray(size - 1)
            for (i in index until size - 1) {
                GLES20.glGenFramebuffers(1, frameBuffers, i)
                GLES20.glGenTextures(1, frameBufferTextures, i)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures!![i])
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                GLES20.glFramebufferTexture2D(
                    GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, frameBufferTextures!![i], 0
                )
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            }
        }
    }

    @SuppressLint("WrongCall")
    override fun onDraw(
        textureId: Int,
        cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        runPendingOnDrawTasks()
        if (!isInitialized() || frameBuffers == null || frameBufferTextures == null) {
            return
        }
        val size = mergedFilters.size
        var previousTexture = textureId
        for (i in 0 until size) {
            val filter = mergedFilters[i]
            val isNotLast = i < size - 1
            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                GLES20.glClearColor(0f, 0f, 0f, 0f)
            }

            /*
            val targetDims = filter.outputDimensions
            if (targetDims != null &&
                (targetDims.first != filter.getOutputWidth()
                        || targetDims.second != filter.getOutputHeight())
            ) {
                onOutputSizeChangedAfter(targetDims.first, targetDims.second, i)
            }
             */

            when (i) {
                0 -> {
                    filter.onDraw(previousTexture, cubeBuffer, textureBuffer)
                }

                size - 1 -> {
                    filter.onDraw(
                        previousTexture, glCubeBuffer,
                        (if (size % 2 == 0) glTextureFlipBuffer else glTextureBuffer)
                    )
                }

                else -> {
                    filter.onDraw(previousTexture, glCubeBuffer, glTextureBuffer)
                }
            }
            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                previousTexture = frameBufferTextures!![i]
            }
        }
    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    fun getFilters(): List<GPUImageFilter> {
        return filters
    }

    fun getMergedFilters(): List<GPUImageFilter> {
        return mergedFilters
    }

    fun updateMergedFilters() {
        mergedFilters.clear()
        var filters: List<GPUImageFilter>?
        for (filter in this.filters) {
            if (filter is GPUImageFilterGroup) {
                filter.updateMergedFilters()
                filters = filter.getMergedFilters()
                if (filters.isEmpty()) continue
                mergedFilters.addAll(filters)
            } else {
                mergedFilters.add(filter)
            }
            val outputDimensions = filter.outputDimensions
            if (outputDimensions != null) {
                this.outputDimensions.let {
                    if (null == it
                        || outputDimensions.first != it.first
                        || outputDimensions.second != it.second
                    ) this.outputDimensions = outputDimensions
                }
            }
        }
    }
}