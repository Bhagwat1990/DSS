package com.dss.controller;

import com.dss.entity.StockData;
import com.dss.service.Nifty50LiveDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the Nifty 50 live data feed.
 *
 * <ul>
 *   <li>POST /api/nifty50/live/start?intervalMs=1000  — start scheduled feed</li>
 *   <li>POST /api/nifty50/live/stop                   — stop scheduled feed</li>
 *   <li>GET  /api/nifty50/live/status                 — feed status &amp; stats</li>
 *   <li>POST /api/nifty50/live/fetch-once             — one-shot fetch for all 50 stocks</li>
 *   <li>GET  /api/nifty50/symbols                     — list Nifty 50 symbols</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/nifty50")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class Nifty50Controller {

    private final Nifty50LiveDataService liveDataService;

    /**
     * Start the live data feed.
     * @param intervalMs polling interval in milliseconds (default 1000)
     */
    @PostMapping("/live/start")
    public ResponseEntity<Map<String, Object>> startFeed(
            @RequestParam(defaultValue = "1000") long intervalMs) {
        liveDataService.start(intervalMs);
        return ResponseEntity.ok(liveDataService.getStatus());
    }

    /**
     * Stop the live data feed.
     */
    @PostMapping("/live/stop")
    public ResponseEntity<Map<String, Object>> stopFeed() {
        liveDataService.stop();
        return ResponseEntity.ok(liveDataService.getStatus());
    }

    /**
     * Get current feed status and statistics.
     */
    @GetMapping("/live/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(liveDataService.getStatus());
    }

    /**
     * One-shot: fetch live data for all 50 stocks and store immediately.
     */
    @PostMapping("/live/fetch-once")
    public ResponseEntity<List<StockData>> fetchOnce() {
        List<StockData> data = liveDataService.fetchOnce();
        return ResponseEntity.ok(data);
    }

    /**
     * List all Nifty 50 constituent symbols.
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getSymbols() {
        return ResponseEntity.ok(liveDataService.getSymbols());
    }
}
