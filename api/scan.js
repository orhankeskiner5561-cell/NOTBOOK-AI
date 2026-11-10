// Vercel serverless function for NOTBOOK AI.
// Requires GEMINI_API_KEY and NOTEBOOK_API_TOKEN as server-side environment variables.
// Do not deploy publicly without authentication and request limits.
const MAX_IMAGE_B64 = 7_000_000;
export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "POST") return res.status(405).json({error:"method_not_allowed"});
  const token = process.env.NOTEBOOK_API_TOKEN;
  const key = process.env.GEMINI_API_KEY;
  if (!token || !key) return res.status(503).json({error:"not_configured"});
  if (req.headers.authorization !== `Bearer ${token}`) return res.status(401).json({error:"unauthorized"});
  const {imageBase64, mimeType} = req.body || {};
  if (!["image/jpeg","image/png","image/webp"].includes(mimeType) ||
      typeof imageBase64 !== "string" || !/^[A-Za-z0-9+/=]+$/.test(imageBase64) ||
      imageBase64.length > MAX_IMAGE_B64 || imageBase64.length < 16) {
    return res.status(400).json({error:"invalid_image"});
  }
  try {
    const model = process.env.GEMINI_MODEL || "gemini-2.5-flash";
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 25000);
    let response;
    try {
      response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`, {
        method:"POST", signal:controller.signal,
        headers:{"Content-Type":"application/json","x-goog-api-key":key},
        body:JSON.stringify({
          contents:[{role:"user",parts:[
            {text:"Türkçe OCR: Görüntüdeki tüm yazıları doğru okuma sırasıyla aktar. Tabloları satır/sütun ilişkisini koruyarak düz metin olarak yaz. Sayı, tarih, saat, tartım ve tutar uydurma. Okunamayan yerleri [OKUNAMADI] olarak işaretle. Sadece JSON döndür: {text:string,warnings:string[],needsReview:boolean}."},
            {inlineData:{mimeType,data:imageBase64}}
          ]}],
          generationConfig:{responseMimeType:"application/json"}
        })
      });
    } finally {clearTimeout(timeout);}
    if (!response.ok) return res.status(502).json({error:"gemini_unavailable"});
    const raw = await response.json();
    const text = raw?.candidates?.[0]?.content?.parts?.map(p=>p.text||"").join("") || "";
    const parsed = JSON.parse(text);
    if (typeof parsed.text !== "string") throw new Error("invalid_response");
    return res.status(200).json({
      text:parsed.text,
      warnings:Array.isArray(parsed.warnings)?parsed.warnings.filter(x=>typeof x==="string"):[],
      needsReview:parsed.needsReview !== false
    });
  } catch (e) {return res.status(502).json({error:"processing_failed"});}
}
