"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import {
  Play,
  Square,
  RefreshCw,
  Loader2,
  AlertCircle,
  ArrowUpRight,
  ArrowDownRight,
  Activity,
  Zap,
  BarChart3,
} from "lucide-react";
import { Card, CardTitle } from "@/components/ui/Cards";
import {
  nifty50Start,
  nifty50Stop,
  nifty50Status,
  nifty50FetchOnce,
  type Nifty50Status,
  type Nifty50StockTick,
} from "@/lib/api";

export default function Nifty50Page() {
  const [status, setStatus] = useState<Nifty50Status | null>(null);
  const [ticks, setTicks] = useState<Nifty50StockTick[]>([]);
  const [intervalMs, setIntervalMs] = useState(1000);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("");
  const [sortField, setSortField] = useState<"symbol" | "close" | "volume">("symbol");
  const [sortAsc, setSortAsc] = useState(true);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const refreshStatus = useCallback(async () => {
    try {
      const s = await nifty50Status();
      setStatus(s);
    } catch {
      // silently ignore status polling errors
    }
  }, []);

  // Poll status every 2s when feed is running
  useEffect(() => {
    refreshStatus();
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [refreshStatus]);

  useEffect(() => {
    if (status?.running) {
      pollRef.current = setInterval(refreshStatus, 2000);
    } else {
      if (pollRef.current) clearInterval(pollRef.current);
    }
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [status?.running, refreshStatus]);

  const handleStart = async () => {
    setLoading(true);
    setError("");
    try {
      const s = await nifty50Start(intervalMs);
      setStatus(s);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to start feed");
    }
    setLoading(false);
  };

  const handleStop = async () => {
    setLoading(true);
    setError("");
    try {
      const s = await nifty50Stop();
      setStatus(s);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to stop feed");
    }
    setLoading(false);
  };

  const handleFetchOnce = async () => {
    setFetching(true);
    setError("");
    try {
      const data = await nifty50FetchOnce();
      setTicks(data);
      await refreshStatus();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to fetch data");
    }
    setFetching(false);
  };

  const handleSort = (field: "symbol" | "close" | "volume") => {
    if (sortField === field) {
      setSortAsc(!sortAsc);
    } else {
      setSortField(field);
      setSortAsc(field === "symbol");
    }
  };

  const filteredTicks = ticks
    .filter((t) => t.symbol.toLowerCase().includes(filter.toLowerCase()))
    .sort((a, b) => {
      const mul = sortAsc ? 1 : -1;
      if (sortField === "symbol") return mul * a.symbol.localeCompare(b.symbol);
      return mul * ((a[sortField] ?? 0) - (b[sortField] ?? 0));
    });

  const isRunning = status?.running ?? false;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">Nifty 50 Live Feed</h1>
        <p className="text-sm text-foreground/40 mt-1">
          Real-time data for all Nifty 50 index constituents — stored for AI analysis
        </p>
      </div>

      {/* Controls & Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Feed Control */}
        <Card glow>
          <CardTitle>Feed Control</CardTitle>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-foreground/40 mb-1.5">
                Polling Interval (ms)
              </label>
              <input
                type="number"
                min={100}
                step={100}
                value={intervalMs}
                onChange={(e) => setIntervalMs(Number(e.target.value))}
                disabled={isRunning}
                className="w-full px-4 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors disabled:opacity-40"
              />
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleStart}
                disabled={loading || isRunning}
                className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-green/10 text-green border border-green/20 rounded-lg text-sm font-medium hover:bg-green/20 transition-colors disabled:opacity-40"
              >
                {loading && !isRunning ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Play className="w-4 h-4" />
                )}
                Start
              </button>
              <button
                onClick={handleStop}
                disabled={loading || !isRunning}
                className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-red/10 text-red border border-red/20 rounded-lg text-sm font-medium hover:bg-red/20 transition-colors disabled:opacity-40"
              >
                <Square className="w-4 h-4" />
                Stop
              </button>
            </div>
            <button
              onClick={handleFetchOnce}
              disabled={fetching}
              className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-accent/10 text-accent border border-accent/20 rounded-lg text-sm font-medium hover:bg-accent/20 transition-colors disabled:opacity-40"
            >
              {fetching ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              Fetch Once
            </button>
          </div>
        </Card>

        {/* Feed Status */}
        <Card>
          <CardTitle>Feed Status</CardTitle>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground/60">Status</span>
              <span
                className={`flex items-center gap-1.5 text-sm font-semibold ${
                  isRunning ? "text-green" : "text-foreground/40"
                }`}
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    isRunning ? "bg-green animate-pulse" : "bg-foreground/20"
                  }`}
                />
                {isRunning ? "Running" : "Stopped"}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground/60">Interval</span>
              <span className="text-sm font-mono">{status?.intervalMs ?? "—"}ms</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground/60">Symbols</span>
              <span className="text-sm font-mono">{status?.symbolCount ?? 50}</span>
            </div>
          </div>
        </Card>

        {/* Stats */}
        <Card>
          <CardTitle>Statistics</CardTitle>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground/60 flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5" /> Total Ticks
              </span>
              <span className="text-sm font-mono text-accent">
                {status?.totalTicks?.toLocaleString() ?? "0"}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground/60 flex items-center gap-1.5">
                <AlertCircle className="w-3.5 h-3.5" /> Errors
              </span>
              <span
                className={`text-sm font-mono ${
                  (status?.errors ?? 0) > 0 ? "text-red" : "text-foreground/40"
                }`}
              >
                {status?.errors ?? 0}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground/60 flex items-center gap-1.5">
                <BarChart3 className="w-3.5 h-3.5" /> Last Fetch
              </span>
              <span className="text-sm font-mono">{ticks.length} records</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Error */}
      {error && (
        <div className="flex items-center gap-2 p-3 bg-red/10 border border-red/20 rounded-lg text-red text-sm">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      {/* Data Table */}
      {ticks.length > 0 && (
        <Card>
          <div className="flex items-center justify-between mb-4">
            <CardTitle className="mb-0">
              Live Data ({filteredTicks.length} stocks)
            </CardTitle>
            <input
              type="text"
              placeholder="Filter symbols..."
              value={filter}
              onChange={(e) => setFilter(e.target.value.toUpperCase())}
              className="px-3 py-1.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors w-48"
            />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-foreground/40">
                  <th
                    className="text-left py-2 px-3 cursor-pointer hover:text-foreground transition-colors"
                    onClick={() => handleSort("symbol")}
                  >
                    Symbol {sortField === "symbol" && (sortAsc ? "↑" : "↓")}
                  </th>
                  <th className="text-right py-2 px-3">Open</th>
                  <th className="text-right py-2 px-3">High</th>
                  <th className="text-right py-2 px-3">Low</th>
                  <th
                    className="text-right py-2 px-3 cursor-pointer hover:text-foreground transition-colors"
                    onClick={() => handleSort("close")}
                  >
                    LTP {sortField === "close" && (sortAsc ? "↑" : "↓")}
                  </th>
                  <th
                    className="text-right py-2 px-3 cursor-pointer hover:text-foreground transition-colors"
                    onClick={() => handleSort("volume")}
                  >
                    Volume {sortField === "volume" && (sortAsc ? "↑" : "↓")}
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredTicks.map((tick) => (
                  <tr
                    key={tick.symbol}
                    className="border-b border-border/50 hover:bg-card-hover/50 transition-colors"
                  >
                    <td className="py-2.5 px-3 font-medium text-accent">
                      {tick.symbol}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono">
                      {tick.open?.toFixed(2) ?? "—"}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono">
                      {tick.high?.toFixed(2) ?? "—"}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono">
                      {tick.low?.toFixed(2) ?? "—"}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono font-semibold">
                      {tick.close?.toFixed(2) ?? "—"}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono text-foreground/60">
                      {tick.volume?.toLocaleString() ?? "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
