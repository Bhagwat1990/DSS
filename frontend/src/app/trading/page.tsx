"use client";

import { useState } from "react";
import {
  ArrowUpRight,
  ArrowDownRight,
  Search,
  Loader2,
  AlertCircle,
  CheckCircle,
  ShieldCheck,
  RefreshCw,
} from "lucide-react";
import { Card, CardTitle } from "@/components/ui/Cards";
import {
  getQuote,
  getHistoricalCandles,
  placeOrder,
  type GrowwQuoteResponse,
  type GrowwOrderRequest,
  type HistoricalCandleResponse,
} from "@/lib/api";

export default function TradingPage() {
  const [quoteSymbol, setQuoteSymbol] = useState("RELIANCE");
  const [exchange, setExchange] = useState("NSE");
  const [quote, setQuote] = useState<GrowwQuoteResponse | null>(null);
  const [loadingQuote, setLoadingQuote] = useState(false);
  const [quoteError, setQuoteError] = useState("");

  // Order form
  const [orderForm, setOrderForm] = useState<Partial<GrowwOrderRequest>>({
    tradingSymbol: "RELIANCE",
    quantity: 1,
    price: 0,
    triggerPrice: 0,
    validity: "DAY",
    exchange: "NSE",
    segment: "CASH",
    product: "CNC",
    orderType: "LIMIT",
    transactionType: "BUY",
  });
  const [orderResult, setOrderResult] = useState<{
    status: string;
    message: string;
  } | null>(null);
  const [loadingOrder, setLoadingOrder] = useState(false);
  const [orderError, setOrderError] = useState("");

  // Historical candles
  const [candleSymbol, setCandleSymbol] = useState("NSE-RELIANCE");
  const [candleExchange, setCandleExchange] = useState("NSE");
  const [candleSegment, setCandleSegment] = useState("CASH");
  const [candleInterval, setCandleInterval] = useState("1day");
  const [candleStartTime, setCandleStartTime] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().slice(0, 10) + " 09:15:00";
  });
  const [candleEndTime, setCandleEndTime] = useState(
    () => new Date().toISOString().slice(0, 10) + " 15:30:00"
  );
  const [candleData, setCandleData] = useState<HistoricalCandleResponse | null>(null);
  const [loadingCandles, setLoadingCandles] = useState(false);
  const [candleError, setCandleError] = useState("");

  const fetchQuote = async () => {
    setLoadingQuote(true);
    setQuoteError("");
    try {
      const res = await getQuote(quoteSymbol, exchange);
      setQuote(res);
    } catch (e) {
      setQuoteError(e instanceof Error ? e.message : "Failed to fetch quote");
    }
    setLoadingQuote(false);
  };

  const submitOrder = async () => {
    setLoadingOrder(true);
    setOrderError("");
    setOrderResult(null);
    try {
      const res = await placeOrder({
        tradingSymbol: orderForm.tradingSymbol || "",
        quantity: orderForm.quantity || 0,
        price: orderForm.price || 0,
        triggerPrice: orderForm.triggerPrice || 0,
        validity: orderForm.validity || "DAY",
        exchange: orderForm.exchange || "NSE",
        segment: orderForm.segment || "CASH",
        product: orderForm.product || "CNC",
        orderType: orderForm.orderType || "LIMIT",
        transactionType: orderForm.transactionType || "BUY",
        orderReferenceId: `DSS-${Date.now()}`,
      });
      setOrderResult({
        status: res.status,
        message: res.status === "SUCCESS"
          ? `Order placed! ID: ${res.payload?.growwOrderId || "N/A"}`
          : `Order failed: ${res.error || "Unknown error"}`,
      });
    } catch (e) {
      setOrderError(e instanceof Error ? e.message : "Order placement failed");
    }
    setLoadingOrder(false);
  };

  const fetchCandles = async () => {
    setLoadingCandles(true);
    setCandleError("");
    setCandleData(null);
    try {
      const res = await getHistoricalCandles(
        candleSymbol, candleStartTime, candleEndTime,
        candleInterval, candleExchange, candleSegment
      );
      setCandleData(res);
    } catch (e) {
      setCandleError(e instanceof Error ? e.message : "Failed to fetch candles");
    }
    setLoadingCandles(false);
  };

  const p = quote?.payload;

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold">Trading</h1>
        <p className="text-sm text-foreground/40 mt-1">
          Live quotes and order placement via Groww
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Quote Section */}
        <div className="space-y-4">
          <Card glow>
            <CardTitle>Live Quote</CardTitle>
            <div className="flex gap-3 items-end">
              <div className="flex-1">
                <label className="block text-xs font-medium text-foreground/40 mb-1.5">Symbol</label>
                <input
                  type="text"
                  value={quoteSymbol}
                  onChange={(e) => setQuoteSymbol(e.target.value.toUpperCase())}
                  className="w-full px-4 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                />
              </div>
              <div className="w-24">
                <label className="block text-xs font-medium text-foreground/40 mb-1.5">Exchange</label>
                <select
                  value={exchange}
                  onChange={(e) => setExchange(e.target.value)}
                  className="w-full px-3 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                >
                  <option value="NSE">NSE</option>
                  <option value="BSE">BSE</option>
                </select>
              </div>
              <button
                onClick={fetchQuote}
                disabled={loadingQuote}
                className="flex items-center gap-2 px-5 py-2.5 bg-accent text-background rounded-lg font-medium text-sm hover:bg-accent-dim transition-colors disabled:opacity-40"
              >
                {loadingQuote ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Search className="w-4 h-4" />
                )}
                Get Quote
              </button>
            </div>

            {quoteError && (
              <div className="flex items-center gap-2 mt-3 text-sm text-red">
                <AlertCircle className="w-4 h-4" />
                {quoteError}
              </div>
            )}
          </Card>

          {p && (
            <Card glow>
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h2 className="text-xl font-bold">{quoteSymbol}</h2>
                  <p className="text-sm text-foreground/40">{exchange}</p>
                </div>
                <div className="text-right">
                  <p className="text-3xl font-bold">₹{(p.lastPrice ?? 0).toFixed(2)}</p>
                  <p
                    className={`flex items-center gap-1 text-sm justify-end ${
                      (p.dayChange ?? 0) >= 0 ? "text-green" : "text-red"
                    }`}
                  >
                    {(p.dayChange ?? 0) >= 0 ? (
                      <ArrowUpRight className="w-4 h-4" />
                    ) : (
                      <ArrowDownRight className="w-4 h-4" />
                    )}
                    {(p.dayChange ?? 0) >= 0 ? "+" : ""}
                    {(p.dayChange ?? 0).toFixed(2)} ({(p.dayChangePerc ?? 0).toFixed(2)}%)
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {[
                  { label: "Open", value: `₹${(p.open ?? 0).toFixed(2)}` },
                  { label: "Close", value: `₹${(p.close ?? 0).toFixed(2)}` },
                  { label: "High", value: `₹${(p.high ?? 0).toFixed(2)}`, cls: "text-green" },
                  { label: "Low", value: `₹${(p.low ?? 0).toFixed(2)}`, cls: "text-red" },
                  { label: "Volume", value: `${((p.volume ?? 0) / 1e6).toFixed(2)}M` },
                  { label: "Avg Price", value: `₹${(p.averagePrice ?? 0).toFixed(2)}` },
                  { label: "52W High", value: `₹${(p.week52High ?? 0).toFixed(2)}` },
                  { label: "52W Low", value: `₹${(p.week52Low ?? 0).toFixed(2)}` },
                  { label: "Bid", value: `₹${(p.bidPrice ?? 0).toFixed(2)} × ${p.bidQuantity ?? 0}` },
                  { label: "Market Cap", value: `₹${((p.marketCap ?? 0) / 1e12).toFixed(2)}T` },
                ].map(({ label, value, cls }) => (
                  <div key={label} className="bg-background rounded-lg p-2.5">
                    <p className="text-[10px] text-foreground/30 uppercase">{label}</p>
                    <p className={`text-sm font-medium font-mono ${cls || ""}`}>{value}</p>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>

        {/* Order Section */}
        <div className="space-y-4">
          <Card glow>
            <CardTitle>Place Order</CardTitle>

            {/* Buy/Sell toggle */}
            <div className="flex gap-2 mb-4">
              {(["BUY", "SELL"] as const).map((type) => (
                <button
                  key={type}
                  onClick={() => setOrderForm((f) => ({ ...f, transactionType: type }))}
                  className={`flex-1 py-2.5 rounded-lg font-medium text-sm transition-all ${
                    orderForm.transactionType === type
                      ? type === "BUY"
                        ? "bg-green/10 text-green border border-green/30"
                        : "bg-red/10 text-red border border-red/30"
                      : "bg-background border border-border text-foreground/40"
                  }`}
                >
                  {type}
                </button>
              ))}
            </div>

            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Symbol</label>
                  <input
                    type="text"
                    value={orderForm.tradingSymbol}
                    onChange={(e) =>
                      setOrderForm((f) => ({ ...f, tradingSymbol: e.target.value.toUpperCase() }))
                    }
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Quantity</label>
                  <input
                    type="number"
                    min={1}
                    value={orderForm.quantity}
                    onChange={(e) =>
                      setOrderForm((f) => ({ ...f, quantity: parseInt(e.target.value) || 0 }))
                    }
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Price</label>
                  <input
                    type="number"
                    step="0.05"
                    value={orderForm.price}
                    onChange={(e) =>
                      setOrderForm((f) => ({ ...f, price: parseFloat(e.target.value) || 0 }))
                    }
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Order Type</label>
                  <select
                    value={orderForm.orderType}
                    onChange={(e) => setOrderForm((f) => ({ ...f, orderType: e.target.value }))}
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  >
                    <option value="LIMIT">Limit</option>
                    <option value="MARKET">Market</option>
                    <option value="SL">Stop Loss</option>
                    <option value="SL-M">SL Market</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Product</label>
                  <select
                    value={orderForm.product}
                    onChange={(e) => setOrderForm((f) => ({ ...f, product: e.target.value }))}
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  >
                    <option value="CNC">CNC</option>
                    <option value="MIS">MIS</option>
                    <option value="NRML">NRML</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Exchange</label>
                  <select
                    value={orderForm.exchange}
                    onChange={(e) => setOrderForm((f) => ({ ...f, exchange: e.target.value }))}
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  >
                    <option value="NSE">NSE</option>
                    <option value="BSE">BSE</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-foreground/40 mb-1">Validity</label>
                  <select
                    value={orderForm.validity}
                    onChange={(e) => setOrderForm((f) => ({ ...f, validity: e.target.value }))}
                    className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
                  >
                    <option value="DAY">Day</option>
                    <option value="IOC">IOC</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Order summary */}
            <div className="mt-4 p-3 bg-background rounded-lg border border-border">
              <div className="flex items-center gap-2 text-xs text-foreground/40 mb-2">
                <ShieldCheck className="w-3 h-3" />
                Order Summary
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-foreground/60">
                  {orderForm.transactionType} {orderForm.quantity} × {orderForm.tradingSymbol}
                </span>
                <span className="font-bold">
                  ₹{((orderForm.quantity || 0) * (orderForm.price || 0)).toFixed(2)}
                </span>
              </div>
            </div>

            <button
              onClick={submitOrder}
              disabled={loadingOrder || !orderForm.tradingSymbol || !orderForm.quantity}
              className={`w-full mt-4 flex items-center justify-center gap-2 py-3 rounded-lg font-medium text-sm transition-all disabled:opacity-40 ${
                orderForm.transactionType === "BUY"
                  ? "bg-green text-background hover:opacity-90"
                  : "bg-red text-white hover:opacity-90"
              }`}
            >
              {loadingOrder ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              Place {orderForm.transactionType} Order
            </button>

            {orderError && (
              <div className="flex items-center gap-2 mt-3 text-sm text-red">
                <AlertCircle className="w-4 h-4" />
                {orderError}
              </div>
            )}
            {orderResult && (
              <div
                className={`flex items-center gap-2 mt-3 text-sm ${
                  orderResult.status === "SUCCESS" ? "text-green" : "text-red"
                }`}
              >
                {orderResult.status === "SUCCESS" ? (
                  <CheckCircle className="w-4 h-4" />
                ) : (
                  <AlertCircle className="w-4 h-4" />
                )}
                {orderResult.message}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Historical Candle Data */}
      <Card glow>
        <CardTitle>Historical Candle Data</CardTitle>
        <p className="text-xs text-foreground/40 mb-4">
          Fetch OHLCV data from Groww for backtesting
        </p>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 mb-4">
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1">Groww Symbol</label>
            <input
              type="text"
              value={candleSymbol}
              onChange={(e) => setCandleSymbol(e.target.value.toUpperCase())}
              placeholder="NSE-RELIANCE"
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1">Exchange</label>
            <select
              value={candleExchange}
              onChange={(e) => setCandleExchange(e.target.value)}
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            >
              <option value="NSE">NSE</option>
              <option value="BSE">BSE</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1">Segment</label>
            <select
              value={candleSegment}
              onChange={(e) => setCandleSegment(e.target.value)}
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            >
              <option value="CASH">CASH</option>
              <option value="FNO">FNO</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1">Interval</label>
            <select
              value={candleInterval}
              onChange={(e) => setCandleInterval(e.target.value)}
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            >
              <option value="1minute">1 min</option>
              <option value="5minute">5 min</option>
              <option value="15minute">15 min</option>
              <option value="30minute">30 min</option>
              <option value="1hour">1 hour</option>
              <option value="1day">1 day</option>
              <option value="1week">1 week</option>
              <option value="1month">1 month</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1">Start Time</label>
            <input
              type="text"
              value={candleStartTime}
              onChange={(e) => setCandleStartTime(e.target.value)}
              placeholder="2025-09-01 09:15:00"
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground/40 mb-1">End Time</label>
            <input
              type="text"
              value={candleEndTime}
              onChange={(e) => setCandleEndTime(e.target.value)}
              placeholder="2025-09-24 15:30:00"
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground focus:outline-none focus:border-accent/50 transition-colors"
            />
          </div>
        </div>

        <button
          onClick={fetchCandles}
          disabled={loadingCandles || !candleSymbol}
          className="flex items-center gap-2 px-5 py-2.5 bg-accent text-background rounded-lg font-medium text-sm hover:bg-accent-dim transition-colors disabled:opacity-40"
        >
          {loadingCandles ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Search className="w-4 h-4" />
          )}
          Fetch Candles
        </button>

        {candleError && (
          <div className="flex items-center gap-2 mt-3 text-sm text-red">
            <AlertCircle className="w-4 h-4" />
            {candleError}
          </div>
        )}

        {candleData?.payload && (
          <div className="mt-4">
            <div className="flex items-center gap-4 mb-3 text-xs text-foreground/40">
              <span>{candleData.payload.candles.length} candles</span>
              <span>Close: ₹{(candleData.payload.closingPrice ?? 0).toFixed(2)}</span>
              <span>{candleData.payload.startTime} → {candleData.payload.endTime}</span>
            </div>

            <div className="overflow-auto max-h-96 rounded-lg border border-border">
              <table className="w-full text-sm">
                <thead className="bg-card sticky top-0">
                  <tr className="text-foreground/40 text-xs">
                    <th className="text-left px-3 py-2 font-medium">Timestamp</th>
                    <th className="text-right px-3 py-2 font-medium">Open</th>
                    <th className="text-right px-3 py-2 font-medium">High</th>
                    <th className="text-right px-3 py-2 font-medium">Low</th>
                    <th className="text-right px-3 py-2 font-medium">Close</th>
                    <th className="text-right px-3 py-2 font-medium">Volume</th>
                  </tr>
                </thead>
                <tbody>
                  {candleData.payload.candles.map((c, i) => {
                    const [ts, open, high, low, close, vol] = c;
                    const bullish = close >= open;
                    return (
                      <tr key={i} className="border-t border-border hover:bg-background/50">
                        <td className="px-3 py-1.5 font-mono text-foreground/60">{ts}</td>
                        <td className="px-3 py-1.5 text-right font-mono">{(open ?? 0).toFixed(2)}</td>
                        <td className="px-3 py-1.5 text-right font-mono text-green">{(high ?? 0).toFixed(2)}</td>
                        <td className="px-3 py-1.5 text-right font-mono text-red">{(low ?? 0).toFixed(2)}</td>
                        <td className={`px-3 py-1.5 text-right font-mono font-medium ${bullish ? "text-green" : "text-red"}`}>
                          {(close ?? 0).toFixed(2)}
                        </td>
                        <td className="px-3 py-1.5 text-right font-mono text-foreground/60">
                          {(vol ?? 0).toLocaleString()}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
