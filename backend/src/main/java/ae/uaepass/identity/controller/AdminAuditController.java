package ae.uaepass.identity.controller;

import ae.uaepass.identity.entity.AuditLog;
import ae.uaepass.identity.repository.AuditLogRepository;
import ae.uaepass.identity.util.PiiMaskingUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin-only read-only audit log viewer.
 * PII is masked in all responses — even admin cannot see raw PII.
 */
@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Query audit logs with pagination.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID userId) {

        size = Math.min(size, 100); // Cap page size

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogRepository.findAll(pageRequest);

        List<Map<String, Object>> masked = logs.getContent().stream()
            .map(this::maskAuditLog)
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "content", masked,
            "totalElements", logs.getTotalElements(),
            "totalPages", logs.getTotalPages(),
            "page", page
        ));
    }

    /**
     * Get specific audit entry by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAuditEntry(@PathVariable Long id) {
        AuditLog entry = auditLogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Audit entry not found"));
        return ResponseEntity.ok(maskAuditLog(entry));
    }

    private Map<String, Object> maskAuditLog(AuditLog log) {
        return Map.of(
            "id", log.getId(),
            "eventType", log.getEventType(),
            "userId", log.getUserId() != null ? PiiMaskingUtil.maskUuid(log.getUserId().toString()) : "N/A",
            "requestId", log.getRequestId() != null ? log.getRequestId() : "N/A",
            "ipAddress", log.getIpAddress() != null ? PiiMaskingUtil.maskIp(log.getIpAddress()) : "N/A",
            "createdAt", log.getCreatedAt().toString(),
            "details", log.getDetails() != null ? log.getDetails() : "{}"
        );
    }
}
