"use client";

import { useState, useEffect, useCallback } from "react";
import {
  TrendingUp,
  TrendingDown,
  BarChart3,
  Activity,
  Zap,
  Database,
} from "lucide-react";
import { Card, CardTitle, StatCard, SignalBadge, LoadingSpinner } from "@/components/ui/Cards";
import { StockPriceChart, MiniSparkline } from "@/components/charts/StockCharts";
import dynamic from "next/dynamic";
import { getStockData, getAdvisorSuggestion, type StockDataPoint, type AdvisorResponse } from "@/lib/api";

const HeroScene = dynamic(
  () => import("@/components/three/FuturisticScene").then((m) => m.HeroScene),
  { ssr: false }
);

const watchlist = ["RELIANCE.BSE", "TCS.BSE", "INFY.BSE", "HDFCBANK.BSE"];

interface WatchlistItem {
  symbol: string;
  data: StockDataPoint[];
  latestClose: number;
  change: number;
  changePercent: number;
}

export default function DashboardPage() {
  const [stocks, setStocks] = useState<WatchlistItem[]>([]);
  const [selectedStock, setSelectedStock] = useState<WatchlistItem | null>(null);
  const [latestSignal, setLatestSignal] = useState<AdvisorResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    const results: WatchlistItem[] = [];

    for (const symbol of watchlist) {
      try {
        const res = await getStockData(symbol);
        if (res.data && res.data.length >= 2) {
          const latest = res.data[res.data.length - 1];
          const prev = res.data[res.data.length - 2];
          const change = latest.close - prev.close;
          const changePercent = (change / prev.close) * 100;
          results.push({
            symbol,
            data: res.data,
            latestClose: latest.close,
            change,
            changePercent,
          });
        }
      } catch {
        // Stock not fetched yet — skip
      }
    }

    setStocks(results);
    if (results.length > 0) {
      setSelectedStock(results[0]);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  useEffect(() => {
    if (!selectedStock) return;
    getAdvisorSuggestion(selectedStock.symbol)
      .then(setLatestSignal)
      .catch(() => setLatestSignal(null));
  }, [selectedStock]);

  return (
    <div className="space-y-6 animate-fade-in relative">
      <HeroScene />

      {/* Header stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Tracked Stocks"
          value={String(stocks.length)}
          icon={<BarChart3 className="w-5 h-5" />}
        />
        <StatCard
          label="Active Signals"
          value={latestSignal ? "1" : "0"}
          change={latestSignal?.signal}
          icon={<Zap className="w-5 h-5" />}
        />
        <StatCard
          label="ML Confidence"
          value={latestSignal ? `${(latestSignal.mlConfidence * 100).toFixed(1)}%` : "—"}
          icon={<Activity className="w-5 h-5" />}
        />
        <StatCard
          label="Data Points"
          value={stocks.reduce((acc, s) => acc + s.data.length, 0).toLocaleString()}
          icon={<Database className="w-5 h-5" />}
        />
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : stocks.length === 0 ? (
        <Card className="text-center py-12">
          <p className="text-foreground/40 mb-2">No stock data loaded yet.</p>
          <p className="text-sm text-foreground/30">
            Use the <strong>Stock Analysis</strong> page to fetch data for your first stock.
          </p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Watchlist panel */}
          <div className="lg:col-span-1 space-y-2">
            <CardTitle>Watchlist</CardTitle>
            {stocks.map((stock) => (
              <button
                key={stock.symbol}
                onClick={() => setSelectedStock(stock)}
                className={`w-full text-left p-3 rounded-lg border transition-all ${
                  selectedStock?.symbol === stock.symbol
                    ? "border-accent/40 bg-accent/5"
                    : "border-border bg-card hover:bg-card-hover"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">
                    {stock.symbol.split(".")[0]}
                  </span>
                  <span
                    className={`flex items-center gap-1 text-xs ${
                      stock.change >= 0 ? "text-green" : "text-red"
                    }`}
                  >
                    {stock.change >= 0 ? (
                      <TrendingUp className="w-3 h-3" />
                    ) : (
                      <TrendingDown className="w-3 h-3" />
                    )}
                    {stock.changePercent.toFixed(2)}%
                  </span>
                </div>
                <p className="text-lg font-bold mt-1">
                  ₹{stock.latestClose.toFixed(2)}
                </p>
                <MiniSparkline
                  data={stock.data.slice(-20).map((d) => ({ value: d.close }))}
                  color={stock.change >= 0 ? "#00ff88" : "#ff4466"}
                  height={30}
                />
              </button>
            ))}
          </div>

          {/* Main chart area */}
          <div className="lg:col-span-3 space-y-4">
            {selectedStock && (
              <>
                <Card glow>
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h2 className="text-xl font-bold">
                        {selectedStock.symbol.split(".")[0]}
                      </h2>
                      <p className="text-sm text-foreground/40">
                        {selectedStock.symbol}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-2xl font-bold">
                        ₹{selectedStock.latestClose.toFixed(2)}
                      </p>
                      <p
                        className={`text-sm ${
                          selectedStock.change >= 0
                            ? "text-green"
                            : "text-red"
                        }`}
                      >
                        {selectedStock.change >= 0 ? "+" : ""}
                        {selectedStock.change.toFixed(2)} (
                        {selectedStock.changePercent.toFixed(2)}%)
                      </p>
                    </div>
                  </div>
                  <StockPriceChart data={selectedStock.data.slice(-60)} />
                </Card>

                {/* Signal card */}
                {latestSignal && (
                  <Card glow className="relative overflow-hidden">
                    <div className="flex items-center gap-4">
                      <SignalBadge signal={latestSignal.signal} />
                      <div className="flex-1">
                        <p className="text-sm text-foreground/60">
                          AI Confidence:{" "}
                          <span className="text-accent font-bold">
                            {(latestSignal.mlConfidence * 100).toFixed(1)}%
                          </span>
                        </p>
                      </div>
                    </div>
                    {latestSignal.aiSuggestion && (
                      <p className="mt-3 text-sm text-foreground/50 leading-relaxed line-clamp-3">
                        {latestSignal.aiSuggestion}
                      </p>
                    )}
                  </Card>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
