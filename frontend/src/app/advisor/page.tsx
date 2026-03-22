"use client";

import { useState } from "react";
import {
  Sparkles,
  Send,
  Loader2,
  AlertCircle,
  TrendingUp,
  TrendingDown,
  Minus,
  Clock,
} from "lucide-react";
import { Card, CardTitle, SignalBadge } from "@/components/ui/Cards";
import dynamic from "next/dynamic";
import { getAdvisorSuggestion, type AdvisorResponse } from "@/lib/api";

const SignalScene = dynamic(
  () => import("@/components/three/FuturisticScene").then((m) => m.SignalScene),
  { ssr: false }
);

export default function AdvisorPage() {
  const [symbol, setSymbol] = useState("RELIANCE.BSE");
  const [context, setContext] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AdvisorResponse | null>(null);
  const [history, setHistory] = useState<AdvisorResponse[]>([]);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await getAdvisorSuggestion(symbol, context || undefined);
      setResult(res);
      setHistory((prev) => [res, ...prev].slice(0, 10));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to get suggestion");
    }
    setLoading(false);
  };

  const signalIcon = (signal: string) => {
    if (signal.includes("BUY")) return <TrendingUp className="w-5 h-5" />;
    if (signal.includes("SELL")) return <TrendingDown className="w-5 h-5" />;
    return <Minus className="w-5 h-5" />;
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold">AI Trading Advisor</h1>
        <p className="text-sm text-foreground/40 mt-1">
          Get AI-powered trading signals combining technical analysis, ML predictions & LLM insights
        </p>
      </div>

      {/* Input form */}
      <Card glow>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="flex flex-wrap gap-4">
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
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1.5">
              Additional Context (optional)
            </label>
            <textarea
              value={context}
              onChange={(e) => setContext(e.target.value)}
              rows={3}
              className="w-full px-4 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors resize-none"
              placeholder="e.g., RBI interest rate decision expected tomorrow, Q3 earnings beat estimates..."
            />
          </div>
          <button
            type="submit"
            disabled={loading || !symbol}
            className="flex items-center gap-2 px-6 py-2.5 bg-accent text-background rounded-lg font-medium text-sm hover:bg-accent-dim transition-colors disabled:opacity-40"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Sparkles className="w-4 h-4" />
            )}
            Get AI Suggestion
          </button>
        </form>
      </Card>

      {error && (
        <div className="flex items-center gap-2 px-4 py-2 bg-red/5 border border-red/20 rounded-lg text-sm text-red">
          <AlertCircle className="w-4 h-4" />
          {error}
        </div>
      )}

      {/* Result */}
      {result && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Signal visualization */}
          <Card glow className="lg:col-span-1 flex flex-col items-center justify-center">
            <SignalScene signal={result.signal} />
            <div className="text-center mt-2">
              <SignalBadge signal={result.signal} />
              <div className="flex items-center gap-2 mt-3 justify-center">
                {signalIcon(result.signal)}
                <span className="text-xl font-bold">{result.symbol.split(".")[0]}</span>
              </div>
              <p className="text-xs text-foreground/40 mt-2">
                ML Prediction: {result.mlPrediction === 1 ? "UP" : "DOWN"}
              </p>
            </div>
          </Card>

          {/* AI Response */}
          <Card glow className="lg:col-span-2">
            <CardTitle>AI Analysis</CardTitle>
            <div className="flex items-center gap-4 mb-4">
              <div className="bg-accent/10 px-4 py-2 rounded-lg">
                <p className="text-xs text-foreground/40">Confidence</p>
                <p className="text-2xl font-bold text-accent">
                  {(result.mlConfidence * 100).toFixed(1)}%
                </p>
              </div>
              <div className="bg-purple/10 px-4 py-2 rounded-lg">
                <p className="text-xs text-foreground/40">Signal</p>
                <p className="text-2xl font-bold text-purple">{result.signal.replace("_", " ")}</p>
              </div>
              {result.timestamp && (
                <div className="flex items-center gap-1 text-xs text-foreground/30">
                  <Clock className="w-3 h-3" />
                  {new Date(result.timestamp).toLocaleString()}
                </div>
              )}
            </div>

            {result.aiSuggestion && (
              <div className="bg-background rounded-lg p-4 border border-border">
                <p className="text-sm text-foreground/70 leading-relaxed whitespace-pre-wrap">
                  {result.aiSuggestion}
                </p>
              </div>
            )}

            {result.inputSummary && (
              <details className="mt-4">
                <summary className="text-xs text-foreground/30 cursor-pointer hover:text-foreground/50 transition-colors">
                  Technical Input Summary
                </summary>
                <pre className="mt-2 text-xs text-foreground/40 bg-background p-3 rounded-lg overflow-x-auto font-mono">
                  {result.inputSummary}
                </pre>
              </details>
            )}

            {result.technicals && Object.keys(result.technicals).length > 0 && (
              <div className="mt-4 pt-4 border-t border-border">
                <p className="text-xs font-medium text-foreground/40 mb-2">Technical Indicators</p>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                  {Object.entries(result.technicals).map(([key, val]) => (
                    <div key={key} className="bg-background rounded p-2">
                      <p className="text-[10px] text-foreground/30 uppercase">{key}</p>
                      <p className="text-sm font-mono font-medium">{typeof val === 'number' ? val.toFixed(2) : val}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>
        </div>
      )}

      {/* History */}
      {history.length > 1 && (
        <Card>
          <CardTitle>Recent Suggestions</CardTitle>
          <div className="space-y-2">
            {history.slice(1).map((h, i) => (
              <div
                key={i}
                className="flex items-center justify-between p-3 bg-background rounded-lg"
              >
                <div className="flex items-center gap-3">
                  <SignalBadge signal={h.signal} />
                  <span className="text-sm font-medium">{h.symbol}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm text-foreground/40">
                    {(h.mlConfidence * 100).toFixed(1)}%
                  </span>
                  {h.timestamp && (
                    <span className="text-xs text-foreground/20">
                      {new Date(h.timestamp).toLocaleTimeString()}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
