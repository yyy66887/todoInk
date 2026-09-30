package com.todoink.app.device

/**
 * 硬件阶段的设备传输接口占位（方案 4.2：不让页面依赖 GATT 细节）。
 * Phase 1 不引入 Nordic BLE Library；接入时由实现类封装连接、MTU、分包与校验。
 */
interface DeviceTransport {
    /** 发送一帧完整位图数据；只有设备返回 DISPLAYED 确认才算成功。 */
    suspend fun sendFrame(frameId: Int, payload: ByteArray): Result<Unit>
}
