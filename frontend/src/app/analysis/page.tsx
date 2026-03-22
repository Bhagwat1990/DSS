"use client";

import { useState, useMemo } from "react";
import {
  Download,
  Calculator,
  TrendingUp,
  TrendingDown,
  ArrowRight,
  CheckCircle,
  AlertCircle,
  Loader2,
} from "lucide-react";
import { Card, CardTitle, LoadingSpinner } from "@/components/ui/Cards";
import { StockPriceChart, IndicatorChart } from "@/components/charts/StockCharts";
import {
  fetchStockData,
  calculateIndicators,
  getStockData,
  type StockDataPoint,
} from "@/lib/api";

type Step = "idle" | "fetching" | "fetched" | "calculating" | "done" | "error";

export default function AnalysisPage() {
  const [symbol, setSymbol] = useState("RELIANCE.BSE");
  const [outputSize, setOutputSize] = useState<"compact" | "full">("compact");
  const [step, setStep] = useState<Step>("idle");
  const [stockData, setStockData] = useState<StockDataPoint[]>([]);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [chartRange, setChartRange] = useState<"1W" | "1M" | "3M" | "6M" | "1Y" | "ALL">("3M");

  const chartSlice = useMemo(() => {
    const rangeMap: Record<string, number> = { "1W": 5, "1M": 22, "3M": 66, "6M": 132, "1Y": 252, ALL: Infinity };
    const n = rangeMap[chartRange] ?? 66;
    return n >= stockData.length ? stockData : stockData.slice(-n);
  }, [stockData, chartRange]);

  const handleFetch = async () => {
    setStep("fetching");
    setError("");
    try {
      const res = await fetchStockData(symbol, outputSize);
      setStockData(res.data || []);
      setMessage(`Fetched ${res.recordCount} records (${res.fromDate} → ${res.toDate})`);
      setStep("fetched");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to fetch stock data");
      setStep("error");
    }
  };

  const handleIndicators = async () => {
    setStep("calculating");
    setError("");
    try {
      const res = await calculateIndicators(symbol);
      setMessage(`Calculated ${res.indicatorsCalculated} indicator records`);
      // Reload data to reflect any updates
      try {
        const updated = await getStockData(symbol);
        setStockData(updated.data || stockData);
      } catch {
        // Keep existing data
      }
      setStep("done");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to calculate indicators");
      setStep("error");
    }
  };

  const ohlcStats = stockData.length > 0
    ? {
        latest: stockData[stockData.length - 1],
        high52: Math.max(...stockData.slice(-252).map((d) => d.high)),
        low52: Math.min(...stockData.slice(-252).map((d) => d.low)),
        avgVol: Math.round(
          stockData.slice(-20).reduce((a, d) => a + d.volume, 0) / Math.min(20, stockData.length)
        ),
      }
    : null;

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold">Stock Analysis</h1>
        <p className="text-sm text-foreground/40 mt-1">
          Fetch market data and compute technical indicators
        </p>
      </div>

      {/* Input controls */}
      <Card>
        <div className="flex flex-wrap gap-4 items-end">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-xs font-medium text-foreground/40 mb-1.5">
              Symbol
            </label>
            <input
              type="text"
              value={symbol}
              onChange={(e) => setSymbol(e.target.value.toUpperCase())}
              className="w-full px-4 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
              placeholder="e.g., RELIANCE.BSE"
            />
          </div>
          <div className="w-40">
            <label className="block text-xs font-medium text-foreground/40 mb-1.5">
              Data Size
            </label>
            <select
              value={outputSize}
              onChange={(e) => setOutputSize(e.target.value as "compact" | "full")}
              className="w-full px-4 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            >
              <option value="compact">Compact (100)</option>
              <option value="full">Full (20yr)</option>
            </select>
          </div>
          <button
            onClick={handleFetch}
            disabled={step === "fetching" || !symbol}
            className="flex items-center gap-2 px-5 py-2.5 bg-accent text-background rounded-lg font-medium text-sm hover:bg-accent-dim transition-colors disabled:opacity-40"
          >
            {step === "fetching" ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Download className="w-4 h-4" />
            )}
            Fetch Data
          </button>
          <button
            onClick={handleIndicators}
            disabled={step !== "fetched" && step !== "done"}
            className="flex items-center gap-2 px-5 py-2.5 bg-purple text-white rounded-lg font-medium text-sm hover:opacity-90 transition-colors disabled:opacity-40"
          >
            {step === "calculating" ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Calculator className="w-4 h-4" />
            )}
            Calculate Indicators
          </button>
        </div>

        {/* Pipeline progress */}
        <div className="flex items-center gap-3 mt-4 pt-4 border-t border-border">
          {(["Fetch Data", "Indicators"] as const).map((label, i) => {
            const active =
              (i === 0 && (step === "fetching")) ||
              (i === 1 && step === "calculating");
            const done =
              (i === 0 && step !== "idle" && step !== "fetching" && step !== "error") ||
              (i === 1 && step === "done");
            return (
              <div key={label} className="flex items-center gap-2">
                {i > 0 && <ArrowRight className="w-3 h-3 text-foreground/20" />}
                <div
                  className={`flex items-center gap-1.5 text-xs font-medium px-3 py-1 rounded-full border ${
                    done
                      ? "border-green/30 text-green bg-green/5"
                      : active
                      ? "border-accent/30 text-accent bg-accent/5"
                      : "border-border text-foreground/30"
                  }`}
                >
                  {done ? (
                    <CheckCircle className="w-3 h-3" />
                  ) : active ? (
                    <Loader2 className="w-3 h-3 animate-spin" />
                  ) : null}
                  {label}
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      {/* Status messages */}
      {message && (
        <div className="flex items-center gap-2 px-4 py-2 bg-accent/5 border border-accent/20 rounded-lg text-sm text-accent">
          <CheckCircle className="w-4 h-4" />
          {message}
        </div>
      )}
      {error && (
        <div className="flex items-center gap-2 px-4 py-2 bg-red/5 border border-red/20 rounded-lg text-sm text-red">
          <AlertCircle className="w-4 h-4" />
          {error}
        </div>
      )}

      {/* Stock data stats */}
      {ohlcStats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card>
            <p className="text-xs text-foreground/40">Last Close</p>
            <p className="text-xl font-bold mt-1">₹{ohlcStats.latest.close.toFixed(2)}</p>
          </Card>
          <Card>
            <p className="text-xs text-foreground/40">52W High</p>
            <p className="text-xl font-bold mt-1 text-green">₹{ohlcStats.high52.toFixed(2)}</p>
          </Card>
          <Card>
            <p className="text-xs text-foreground/40">52W Low</p>
            <p className="text-xl font-bold mt-1 text-red">₹{ohlcStats.low52.toFixed(2)}</p>
          </Card>
          <Card>
            <p className="text-xs text-foreground/40">Avg Volume (20d)</p>
            <p className="text-xl font-bold mt-1">{(ohlcStats.avgVol / 1e6).toFixed(2)}M</p>
          </Card>
        </div>
      )}

      {/* Main chart */}
      {stockData.length > 0 && (
        <Card glow>
          <div className="flex items-center justify-between mb-2">
            <CardTitle>Price & Volume Chart</CardTitle>
            <div className="flex items-center gap-1 bg-background/60 rounded-full p-0.5 border border-border/40">
              {(["1W", "1M", "3M", "6M", "1Y", "ALL"] as const).map((range) => (
                <button
                  key={range}
                  onClick={() => setChartRange(range)}
                  className={`px-3 py-1 rounded-full text-[11px] font-semibold tracking-wide transition-all ${
                    chartRange === range
                      ? "bg-accent text-white shadow-sm shadow-accent/30"
                      : "text-foreground/40 hover:text-foreground/70"
                  }`}
                >
                  {range}
                </button>
              ))}
            </div>
          </div>
          <StockPriceChart data={chartSlice} height={480} />
        </Card>
      )}

      {/* OHLC table */}
      {stockData.length > 0 && (
        <Card>
          <CardTitle>Recent OHLCV Data</CardTitle>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-2 px-3 text-foreground/40 font-medium">Date</th>
                  <th className="text-right py-2 px-3 text-foreground/40 font-medium">Open</th>
                  <th className="text-right py-2 px-3 text-foreground/40 font-medium">High</th>
                  <th className="text-right py-2 px-3 text-foreground/40 font-medium">Low</th>
                  <th className="text-right py-2 px-3 text-foreground/40 font-medium">Close</th>
                  <th className="text-right py-2 px-3 text-foreground/40 font-medium">Volume</th>
                  <th className="text-right py-2 px-3 text-foreground/40 font-medium">Change</th>
                </tr>
              </thead>
              <tbody>
                {stockData
                  .slice(-15)
                  .reverse()
                  .map((d, i, arr) => {
                    const prev = arr[i + 1];
                    const change = prev && prev.close ? ((d.close - prev.close) / prev.close) * 100 : 0;
                    return (
                      <tr key={d.date} className="border-b border-border/50 hover:bg-card-hover transition-colors">
                        <td className="py-2 px-3 font-mono text-foreground/60">{d.date}</td>
                        <td className="py-2 px-3 text-right">{d.open?.toFixed(2) ?? "—"}</td>
                        <td className="py-2 px-3 text-right text-green/80">{d.high?.toFixed(2) ?? "—"}</td>
                        <td className="py-2 px-3 text-right text-red/80">{d.low?.toFixed(2) ?? "—"}</td>
                        <td className="py-2 px-3 text-right font-medium">{d.close?.toFixed(2) ?? "—"}</td>
                        <td className="py-2 px-3 text-right text-foreground/50">
                          {d.volume != null ? (d.volume / 1e6).toFixed(2) + "M" : "—"}
                        </td>
                        <td className={`py-2 px-3 text-right ${change >= 0 ? "text-green" : "text-red"}`}>
                          {change >= 0 ? (
                            <TrendingUp className="w-3 h-3 inline mr-1" />
                          ) : (
                            <TrendingDown className="w-3 h-3 inline mr-1" />
                          )}
                          {change.toFixed(2)}%
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
