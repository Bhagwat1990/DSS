"use client";

import { useMemo, useCallback } from "react";
import {
  ResponsiveContainer,
  ComposedChart,
  Line,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Area,
  ReferenceLine,
  Cell,
} from "recharts";
import type { StockDataPoint } from "@/lib/api";

/* ───────────── Candlestick Bar Shape ───────────── */
interface CandlePayload {
  open: number;
  close: number;
  high: number;
  low: number;
}

function CandlestickShape(props: {
  x: number;
  y: number;
  width: number;
  height: number;
  payload: CandlePayload;
  yAxis: { scale: (v: number) => number };
}) {
  const { x, width, payload, yAxis } = props;
  if (!payload || payload.open == null || payload.close == null) return null;

  const bullish = payload.close >= payload.open;
  const fill = bullish ? "#22c55e" : "#ef4444";

  const bodyTop = yAxis.scale(Math.max(payload.open, payload.close));
  const bodyBottom = yAxis.scale(Math.min(payload.open, payload.close));
  const bodyHeight = Math.max(bodyBottom - bodyTop, 1);

  const wickTop = yAxis.scale(payload.high);
  const wickBottom = yAxis.scale(payload.low);
  const cx = x + width / 2;

  return (
    <g>
      <line x1={cx} y1={wickTop} x2={cx} y2={wickBottom} stroke={fill} strokeWidth={1} />
      <rect
        x={x + width * 0.15}
        y={bodyTop}
        width={width * 0.7}
        height={bodyHeight}
        fill={fill}
        rx={1}
      />
    </g>
  );
}

/* ───────────── Custom Tooltip ───────────── */
function ChartTooltip({ active, payload, label }: { active?: boolean; payload?: Array<{ payload: StockDataPoint }>; label?: string }) {
  if (!active || !payload?.length) return null;
  const d = payload[0].payload;
  const bullish = d.close >= d.open;
  const change = d.open ? ((d.close - d.open) / d.open) * 100 : 0;

  return (
    <div className="rounded-xl border border-white/10 bg-[#0f1117]/95 backdrop-blur-md px-4 py-3 shadow-2xl">
      <p className="text-[11px] text-white/40 mb-2 font-medium tracking-wide">{label}</p>
      <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-[12px]">
        <span className="text-white/50">Open</span>
        <span className="text-right text-white font-medium">{d.open?.toFixed(2)}</span>
        <span className="text-white/50">High</span>
        <span className="text-right text-emerald-400 font-medium">{d.high?.toFixed(2)}</span>
        <span className="text-white/50">Low</span>
        <span className="text-right text-red-400 font-medium">{d.low?.toFixed(2)}</span>
        <span className="text-white/50">Close</span>
        <span className={`text-right font-semibold ${bullish ? "text-emerald-400" : "text-red-400"}`}>
          {d.close?.toFixed(2)}
        </span>
        <span className="text-white/50">Volume</span>
        <span className="text-right text-white/70 font-medium">
          {d.volume != null ? `${(d.volume / 1e6).toFixed(2)}M` : "—"}
        </span>
      </div>
      <div className={`mt-2 pt-2 border-t border-white/10 text-center text-[12px] font-semibold ${bullish ? "text-emerald-400" : "text-red-400"}`}>
        {bullish ? "▲" : "▼"} {Math.abs(change).toFixed(2)}%
      </div>
    </div>
  );
}

/* ───────────── Main Price & Volume Chart ───────────── */
interface StockChartProps {
  data: StockDataPoint[];
  showVolume?: boolean;
  height?: number;
}

export function StockPriceChart({ data, showVolume = true, height = 400 }: StockChartProps) {
  const chartData = useMemo(
    () =>
      data.map((d) => ({
        ...d,
        date: d.date.length > 10 ? d.date.slice(0, 10) : d.date,
      })),
    [data]
  );

  const { minPrice, maxPrice, avgClose, maxVol } = useMemo(() => {
    const prices = chartData.filter(
      (d) => d.low != null && d.high != null && d.close != null
    );
    const lows = prices.map((d) => d.low);
    const highs = prices.map((d) => d.high);
    const allClose = prices.map((d) => d.close);
    return {
      minPrice: Math.min(...lows) * 0.997,
      maxPrice: Math.max(...highs) * 1.003,
      avgClose: allClose.length
        ? allClose.reduce((s, v) => s + v, 0) / allClose.length
        : 0,
      maxVol: Math.max(...chartData.map((d) => d.volume ?? 0)),
    };
  }, [chartData]);

  const formatYAxis = useCallback(
    (v: number) => (v >= 1000 ? `${(v / 1000).toFixed(1)}k` : v.toFixed(0)),
    []
  );

  return (
    <div className="relative">
      {/* Price header pill */}
      {chartData.length > 0 && (
        <div className="flex items-center gap-3 mb-4">
          {(() => {
            const last = chartData[chartData.length - 1];
            const first = chartData[0];
            const change = first?.close ? ((last.close - first.close) / first.close) * 100 : 0;
            const bullish = change >= 0;
            return (
              <>
                <span className="text-2xl font-bold text-white tracking-tight">
                  ₹{last.close?.toFixed(2)}
                </span>
                <span
                  className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                    bullish
                      ? "bg-emerald-500/15 text-emerald-400"
                      : "bg-red-500/15 text-red-400"
                  }`}
                >
                  {bullish ? "▲" : "▼"} {Math.abs(change).toFixed(2)}%
                </span>
              </>
            );
          })()}
        </div>
      )}

      <ResponsiveContainer width="100%" height={height}>
        <ComposedChart
          data={chartData}
          margin={{ top: 8, right: 8, bottom: 0, left: -8 }}
        >
          <defs>
            <linearGradient id="volGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#6366f1" stopOpacity={0.5} />
              <stop offset="100%" stopColor="#6366f1" stopOpacity={0.05} />
            </linearGradient>
          </defs>

          <CartesianGrid
            vertical={false}
            stroke="#1e293b"
            strokeDasharray="none"
            strokeOpacity={0.5}
          />

          <XAxis
            dataKey="date"
            tick={{ fill: "#475569", fontSize: 10, fontFamily: "var(--font-sans)" }}
            tickLine={false}
            axisLine={false}
            interval="preserveStartEnd"
            dy={8}
          />

          <YAxis
            yAxisId="price"
            domain={[minPrice, maxPrice]}
            tick={{ fill: "#475569", fontSize: 10, fontFamily: "var(--font-sans)" }}
            tickLine={false}
            axisLine={false}
            tickFormatter={formatYAxis}
            width={55}
          />

          {showVolume && (
            <YAxis
              yAxisId="volume"
              orientation="right"
              domain={[0, maxVol * 4]}
              tick={false}
              tickLine={false}
              axisLine={false}
              width={0}
            />
          )}

          <Tooltip
            content={<ChartTooltip />}
            cursor={{ stroke: "#475569", strokeDasharray: "4 4" }}
          />

          {/* Volume bars behind candles */}
          {showVolume && (
            <Bar yAxisId="volume" dataKey="volume" fill="url(#volGrad)" barSize={6} isAnimationActive={false}>
              {chartData.map((d, i) => (
                <Cell
                  key={i}
                  fill={d.close >= d.open ? "rgba(34,197,94,0.18)" : "rgba(239,68,68,0.18)"}
                />
              ))}
            </Bar>
          )}

          {/* Candlestick bodies */}
          <Bar
            yAxisId="price"
            dataKey="high"
            shape={(props: any) => (
              <CandlestickShape {...props} yAxis={props.yAxis ?? { scale: (v: number) => v }} />
            )}
            isAnimationActive={false}
          />

          {/* Moving average line (close) */}
          <Line
            yAxisId="price"
            type="monotone"
            dataKey="close"
            stroke="#00d4ff"
            strokeWidth={1.5}
            dot={false}
            strokeOpacity={0.4}
            name="close"
          />

          {/* Reference lines */}
          <ReferenceLine
            yAxisId="price"
            y={avgClose}
            stroke="#facc15"
            strokeDasharray="6 4"
            strokeOpacity={0.5}
            label={{
              value: `AVG ${avgClose.toFixed(0)}`,
              fill: "#facc1580",
              fontSize: 9,
              position: "insideTopRight",
            }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}

interface MiniChartProps {
  data: { value: number }[];
  color?: string;
  height?: number;
}

export function MiniSparkline({ data, color = "#00d4ff", height = 40 }: MiniChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={data}>
        <defs>
          <linearGradient id={`spark-${color.replace("#", "")}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.3} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>
        <Area
          dataKey="value"
          stroke={color}
          strokeWidth={1.5}
          fill={`url(#spark-${color.replace("#", "")})`}
          dot={false}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

interface IndicatorChartProps {
  data: { date: string; value: number }[];
  label: string;
  color?: string;
  height?: number;
  referenceLines?: { y: number; label: string; color: string }[];
}

export function IndicatorChart({
  data,
  label,
  color = "#00d4ff",
  height = 150,
  referenceLines,
}: IndicatorChartProps) {
  return (
    <div>
      <p className="text-xs font-medium text-foreground/40 uppercase tracking-wider mb-2">{label}</p>
      <ResponsiveContainer width="100%" height={height}>
        <ComposedChart data={data} margin={{ top: 5, right: 5, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
          <XAxis
            dataKey="date"
            tick={{ fill: "#64748b", fontSize: 10 }}
            tickLine={false}
            axisLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            tick={{ fill: "#64748b", fontSize: 10 }}
            tickLine={false}
            axisLine={false}
          />
          <Tooltip
            contentStyle={{
              background: "#111827",
              border: "1px solid #1e293b",
              borderRadius: "8px",
              fontSize: "12px",
              color: "#e0e6f0",
            }}
          />
          <Line dataKey="value" stroke={color} strokeWidth={1.5} dot={false} />
          {referenceLines?.map((ref) => (
            <ReferenceLine
              key={ref.label}
              y={ref.y}
              stroke={ref.color}
              strokeDasharray="3 3"
              label={{ value: ref.label, fill: ref.color, fontSize: 10 }}
            />
          ))}
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
