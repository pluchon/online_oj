package cn.nuonuoya.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import java.util.function.Supplier;

// Sentinel 调用保护：在资源内执行远程调用，异常计入熔断统计后原样抛出；被限流或熔断时抛出 BlockException，由调用方返回明确的降级结果
public final class SentinelGuard {

    private SentinelGuard() {
    }

    // 在指定资源内执行调用
    public static <T> T call(String resource, Supplier<T> action) throws BlockException {
        Entry entry = SphU.entry(resource);
        try {
            return action.get();
        } catch (RuntimeException e) {
            Tracer.traceEntry(e, entry);
            throw e;
        } finally {
            entry.exit();
        }
    }
}
