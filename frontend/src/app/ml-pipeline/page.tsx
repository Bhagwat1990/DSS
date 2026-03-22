"use client";

import { useState } from "react";
import {
  Brain,
  Cpu,
  ArrowRight,
  CheckCircle,
  Loader2,
  AlertCircle,
  Target,
  Layers,
  Zap,
} from "lucide-react";
import { Card, CardTitle } from "@/components/ui/Cards";
import dynamic from "next/dynamic";
import {
  generateFeatures,
  trainModel,
  type FeatureResponse,
  type TrainResponse,
} from "@/lib/api";
import {
  ResponsiveContainer,
  RadialBarChart,
  RadialBar,
  PolarAngleAxis,
} from "recharts";

const WaveBackground = dynamic(
  () => import("@/components/three/FuturisticScene").then((m) => m.WaveBackground),
  { ssr: false }
);

type PipelineStep = "idle" | "features" | "training" | "done" | "error";

export default function MLPipelinePage() {
  const [symbol, setSymbol] = useState("RELIANCE.BSE");
  const [step, setStep] = useState<PipelineStep>("idle");
  const [featureResult, setFeatureResult] = useState<FeatureResponse | null>(null);
  const [trainResult, setTrainResult] = useState<TrainResponse | null>(null);
  const [error, setError] = useState("");

  const handleFeatures = async () => {
    setStep("features");
    setError("");
    try {
      const res = await generateFeatures(symbol);
      setFeatureResult(res);
      setStep("training");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Feature generation failed");
      setStep("error");
    }
  };

  const handleTrain = async () => {
    setStep("training");
    setError("");
    try {
      const res = await trainModel(symbol);
      setTrainResult(res);
      setStep("done");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Model training failed");
      setStep("error");
    }
  };

  const handleRunAll = async () => {
    setStep("features");
    setError("");
    setFeatureResult(null);
    setTrainResult(null);

    try {
      const fRes = await generateFeatures(symbol);
      setFeatureResult(fRes);
      setStep("training");

      const tRes = await trainModel(symbol);
      setTrainResult(tRes);
      setStep("done");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Pipeline failed");
      setStep("error");
    }
  };

  const accuracyData = trainResult
    ? [{ value: trainResult.accuracy, fill: trainResult.accuracy >= 60 ? "#00ff88" : trainResult.accuracy >= 50 ? "#ffd700" : "#ff4466" }]
    : [];

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold">ML Pipeline</h1>
        <p className="text-sm text-foreground/40 mt-1">
          Generate features, train classifiers, and evaluate model performance
        </p>
      </div>

      {/* Controls */}
      <Card className="relative overflow-hidden">
        <WaveBackground />
        <div className="flex flex-wrap gap-4 items-end relative z-10">
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
          <button
            onClick={handleRunAll}
            disabled={step === "features" || step === "training"}
            className="flex items-center gap-2 px-6 py-2.5 bg-accent text-background rounded-lg font-medium text-sm hover:bg-accent-dim transition-colors disabled:opacity-40"
          >
            {step === "features" || step === "training" ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Zap className="w-4 h-4" />
            )}
            Run Full Pipeline
          </button>
        </div>

        {/* Pipeline steps */}
        <div className="flex items-center gap-4 mt-6 pt-4 border-t border-border relative z-10">
          {(
            [
              { key: "features", label: "Feature Engineering", icon: Layers },
              { key: "training", label: "Model Training", icon: Brain },
              { key: "done", label: "Evaluation", icon: Target },
            ] as const
          ).map(({ key, label, icon: Icon }, i) => {
            const active =
              step === key ||
              (key === "training" && step === "training" && featureResult !== null);
            const done =
              (key === "features" && featureResult !== null && step !== "features") ||
              (key === "training" && trainResult !== null) ||
              (key === "done" && step === "done");
            return (
              <div key={key} className="flex items-center gap-2">
                {i > 0 && <ArrowRight className="w-3 h-3 text-foreground/20" />}
                <div
                  className={`flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full border ${
                    done
                      ? "border-green/30 text-green bg-green/5"
                      : active
                      ? "border-accent/30 text-accent bg-accent/5 animate-pulse"
                      : "border-border text-foreground/30"
                  }`}
                >
                  {done ? (
                    <CheckCircle className="w-3 h-3" />
                  ) : active ? (
                    <Loader2 className="w-3 h-3 animate-spin" />
                  ) : (
                    <Icon className="w-3 h-3" />
                  )}
                  {label}
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      {/* Error */}
      {error && (
        <div className="flex items-center gap-2 px-4 py-2 bg-red/5 border border-red/20 rounded-lg text-sm text-red">
          <AlertCircle className="w-4 h-4" />
          {error}
        </div>
      )}

      {/* Results grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Feature stats */}
        {featureResult && (
          <Card glow>
            <CardTitle>Feature Engineering</CardTitle>
            <div className="flex items-center gap-4 mt-2">
              <div className="w-14 h-14 rounded-xl bg-purple/10 flex items-center justify-center">
                <Layers className="w-7 h-7 text-purple" />
              </div>
              <div>
                <p className="text-3xl font-bold">{featureResult.featuresGenerated}</p>
                <p className="text-sm text-foreground/40">Feature vectors generated</p>
              </div>
            </div>
            <div className="mt-4 pt-4 border-t border-border">
              <p className="text-xs text-foreground/30">{featureResult.message}</p>
            </div>
            <div className="mt-3 space-y-2">
              {[
                "RSI",
                "SMA Ratio (Short/Long)",
                "MACD & Signal",
                "Bollinger Position",
                "Price Change",
                "Volume Change",
                "ATR",
              ].map((f) => (
                <div key={f} className="flex items-center gap-2 text-xs">
                  <div className="w-1.5 h-1.5 rounded-full bg-accent" />
                  <span className="text-foreground/50">{f}</span>
                </div>
              ))}
            </div>
          </Card>
        )}

        {/* Training results */}
        {trainResult && (
          <Card glow>
            <CardTitle>Model Performance</CardTitle>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-foreground/40 mt-2">Training Records</p>
                <p className="text-2xl font-bold">{trainResult.trainingRecords}</p>
              </div>
              <div className="w-32 h-32">
                <ResponsiveContainer width="100%" height="100%">
                  <RadialBarChart
                    cx="50%"
                    cy="50%"
                    innerRadius="70%"
                    outerRadius="100%"
                    data={accuracyData}
                    startAngle={90}
                    endAngle={-270}
                  >
                    <PolarAngleAxis type="number" domain={[0, 100]} tick={false} />
                    <RadialBar dataKey="value" cornerRadius={10} background={{ fill: "#1e293b" }} />
                    <text
                      x="50%"
                      y="50%"
                      textAnchor="middle"
                      dominantBaseline="middle"
                      className="fill-foreground text-lg font-bold"
                    >
                      {trainResult.accuracy.toFixed(1)}%
                    </text>
                  </RadialBarChart>
                </ResponsiveContainer>
              </div>
            </div>
            <div className="mt-4 pt-4 border-t border-border">
              <p className="text-xs text-foreground/30">{trainResult.message}</p>
            </div>
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div className="bg-background rounded-lg p-3">
                <p className="text-xs text-foreground/40">Algorithm</p>
                <p className="text-sm font-medium mt-1">Random Forest</p>
              </div>
              <div className="bg-background rounded-lg p-3">
                <p className="text-xs text-foreground/40">Split</p>
                <p className="text-sm font-medium mt-1">80/20 Train/Test</p>
              </div>
            </div>
          </Card>
        )}
      </div>

      {/* Pipeline info */}
      <Card>
        <CardTitle>Pipeline Architecture</CardTitle>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-2">
          {[
            {
              icon: Cpu,
              title: "Feature Engineering",
              desc: "Combines RSI, SMA ratios, MACD, Bollinger positions, price/volume changes, and ATR into feature vectors with next-day labels.",
              color: "text-accent",
            },
            {
              icon: Brain,
              title: "Ensemble Classifier",
              desc: "Decision tree / Random Forest ensemble with threshold voting on an 80/20 train-test split for price direction prediction.",
              color: "text-purple",
            },
            {
              icon: Target,
              title: "Signal Generation",
              desc: "ML confidence maps to signals: ≥80% STRONG_BUY, ≥60% BUY, ≥40% HOLD, ≥20% SELL, <20% STRONG_SELL.",
              color: "text-green",
            },
          ].map(({ icon: Icon, title, desc, color }) => (
            <div key={title} className="bg-background rounded-lg p-4">
              <Icon className={`w-6 h-6 ${color} mb-2`} />
              <p className="text-sm font-medium">{title}</p>
              <p className="text-xs text-foreground/40 mt-1 leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
