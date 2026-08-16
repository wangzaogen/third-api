package com.thirdapi.starter.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jCallLogger implements CallLogger {

    private static final Logger log = LoggerFactory.getLogger("THIRD_API_CALL");

    @Override
    public void log(ApiCallLog callLog) {
        if (callLog.isSuccess()) {
            log.info("trace={} provider={} channel={} endpoint={} method={} status={} cost={}ms",
                    callLog.getTraceId(), callLog.getProvider(), callLog.getChannel(),
                    callLog.getEndpoint(), callLog.getHttpMethod(), callLog.getHttpStatus(), callLog.getCostMs());
        } else {
            log.warn("trace={} provider={} channel={} endpoint={} status={} errorType={} error={} cost={}ms",
                    callLog.getTraceId(), callLog.getProvider(), callLog.getChannel(),
                    callLog.getEndpoint(), callLog.getHttpStatus(), callLog.getErrorType(),
                    callLog.getErrorMessage(), callLog.getCostMs());
        }
    }
}
