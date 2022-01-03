package com.github.mtps.ktor

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory

class ExceptionInterceptor : ServerInterceptor {
	override fun <ReqT : Any, RespT : Any> interceptCall(
		call: ServerCall<ReqT, RespT>,
		headers: Metadata,
		next: ServerCallHandler<ReqT, RespT>
	): ServerCall.Listener<ReqT> {
		return next.startCall(ExceptionTranslatingServerCall(call), headers)
	}
}

/**
 * When closing a gRPC call, extract any error status information to top-level fields. Also
 * log the cause of errors.
 */
private class ExceptionTranslatingServerCall<ReqT, RespT>(
	delegate: ServerCall<ReqT, RespT>
) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun close(status: Status, trailers: Metadata) {
		if (status.isOk) {
			return super.close(status, trailers)
		}
		val cause = status.cause
		var newStatus = status

		log.error("Error handling grpc call", cause)

		if (status.code == Status.Code.UNKNOWN) {
			val translatedStatus = when (cause) {
				is IllegalArgumentException -> Status.INVALID_ARGUMENT
				is IllegalStateException -> Status.FAILED_PRECONDITION
				else -> Status.UNKNOWN
			}
			newStatus = translatedStatus.withDescription(cause?.message).withCause(cause)
		}
		super.close(newStatus, trailers)
	}
}
