const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, {
    headers: { "Content-Type": "application/json", ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "Unknown error");
    throw new Error(`API ${res.status}: ${text}`);
  }
  return res.json();
}

// Stock Data
export function fetchStockData(symbol: string, outputSize = "compact") {
  return request<StockDataResponse>("/api/stocks/fetch", {
    method: "POST",
    body: JSON.stringify({ symbol, outputSize }),
  });
}

export function getStockData(symbol: string) {
  return request<StockDataResponse>(`/api/stocks/${encodeURIComponent(symbol)}`);
}

export function calculateIndicators(symbol: string) {
  return request<IndicatorResponse>(`/api/stocks/${encodeURIComponent(symbol)}/indicators`, {
    method: "POST",
  });
}

// ML Pipeline
export function generateFeatures(symbol: string) {
  return request<FeatureResponse>("/api/ml/features", {
    method: "POST",
    body: JSON.stringify({ symbol }),
  });
}

export function trainModel(symbol: string) {
  return request<TrainResponse>("/api/ml/train", {
    method: "POST",
    body: JSON.stringify({ symbol }),
  });
}

// Advisor
export function getAdvisorSuggestion(symbol: string, additionalContext?: string) {
  return request<AdvisorResponse>("/api/advisor/suggest", {
    method: "POST",
    body: JSON.stringify({ symbol, additionalContext }),
  });
}

// Groww
export function getQuote(symbol: string, exchange = "NSE") {
  return request<GrowwQuoteResponse>(
    `/api/groww/quote?symbol=${encodeURIComponent(symbol)}&exchange=${encodeURIComponent(exchange)}`
  );
}

export function getLtp(segment: string, symbols: string) {
  return request<Record<string, number>>(
    `/api/groww/ltp?segment=${encodeURIComponent(segment)}&symbols=${encodeURIComponent(symbols)}`
  );
}

export function getHistoricalCandles(
  growwSymbol: string,
  startTime: string,
  endTime: string,
  candleInterval = "1day",
  exchange = "NSE",
  segment = "CASH"
) {
  return request<HistoricalCandleResponse>(
    `/api/groww/historical/candles?exchange=${encodeURIComponent(exchange)}&segment=${encodeURIComponent(segment)}&growwSymbol=${encodeURIComponent(growwSymbol)}&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}&candleInterval=${encodeURIComponent(candleInterval)}`
  );
}

export function placeOrder(order: GrowwOrderRequest) {
  return request<GrowwOrderResponse>("/api/groww/order", {
    method: "POST",
    body: JSON.stringify(order),
  });
}

// Nifty 50 Live Data
export function nifty50Start(intervalMs = 1000) {
  return request<Nifty50Status>(`/api/nifty50/live/start?intervalMs=${intervalMs}`, {
    method: "POST",
  });
}

export function nifty50Stop() {
  return request<Nifty50Status>("/api/nifty50/live/stop", { method: "POST" });
}

export function nifty50Status() {
  return request<Nifty50Status>("/api/nifty50/live/status");
}

export function nifty50FetchOnce() {
  return request<Nifty50StockTick[]>("/api/nifty50/live/fetch-once", { method: "POST" });
}

export function nifty50Symbols() {
  return request<string[]>("/api/nifty50/symbols");
}

// Types
export interface StockDataPoint {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface StockDataResponse {
  symbol: string;
  recordCount: number;
  fromDate: string;
  toDate: string;
  message: string;
  data: StockDataPoint[];
}

export interface IndicatorResponse {
  symbol: string;
  indicatorsCalculated: number;
  message: string;
}

export interface FeatureResponse {
  symbol: string;
  featuresGenerated: number;
  message: string;
}

export interface TrainResponse {
  symbol: string;
  trainingRecords: number;
  accuracy: number;
  message: string;
}

export interface AdvisorResponse {
  symbol: string;
  signal: "STRONG_BUY" | "BUY" | "HOLD" | "SELL" | "STRONG_SELL";
  mlConfidence: number;
  mlPrediction: number;
  aiSuggestion: string;
  inputSummary: string;
  technicals: Record<string, number>;
  timestamp: string;
}

export interface GrowwQuoteResponse {
  status: string;
  payload: {
    averagePrice: number;
    bidPrice: number;
    bidQuantity: number;
    dayChange: number;
    dayChangePerc: number;
    open: number;
    high: number;
    low: number;
    close: number;
    lastPrice: number;
    volume: number;
    week52High: number;
    week52Low: number;
    marketCap: number;
  };
}

export interface GrowwOrderRequest {
  tradingSymbol: string;
  quantity: number;
  price: number;
  triggerPrice: number;
  validity: string;
  exchange: string;
  segment: string;
  product: string;
  orderType: string;
  transactionType: string;
  orderReferenceId: string;
}

export interface GrowwOrderResponse {
  status: string;
  payload: {
    growwOrderId: string;
    orderStatus: string;
    orderReferenceId: string;
    remark: string;
  };
  error: string;
}

export interface HistoricalCandleResponse {
  status: string;
  payload: {
    /** Each candle: [timestamp, open, high, low, close, volume, openInterest] */
    candles: [string, number, number, number, number, number, number | null][];
    closingPrice: number;
    startTime: string;
    endTime: string;
    intervalInMinutes: number;
  };
}

export interface Nifty50Status {
  running: boolean;
  intervalMs: number;
  totalTicks: number;
  errors: number;
  symbolCount: number;
  symbols: string[];
}

export interface Nifty50StockTick {
  symbol: string;
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}
