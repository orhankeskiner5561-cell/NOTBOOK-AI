# NOTBOOK AI — Gemini entegrasyon sözleşmesi

Bu dosya henüz etkin olmayan Gemini entegrasyonunun güvenli uygulama planıdır.

## Mimari
- Android uygulaması Gemini API anahtarını **asla** içermez.
- Kullanıcı izin verdikten sonra yalnızca seçilen görüntü veya soruyla ilgili kayıtlar HTTPS üzerinden sunucuya gönderilir.
- Sunucu Gemini API'yi çağırır; kota, kimlik doğrulama, istek boyutu ve hız sınırlaması uygular.
- API anahtarı sunucunun gizli ortam değişkeninde tutulur; GitHub'a yazılmaz.
- Sunucu hazır değilse mevcut cihaz içi ML Kit OCR çalışmaya devam eder.
- Görüntü aktarımı varsayılan olarak kapalıdır; açık onay gerekir.
- Hassas belgeler sunucuda kalıcı saklanmaz; istek günlüğüne ham belge yazılmaz.

## POST /api/scan
Girdi (kimliği doğrulanmış kullanıcı, JSON):
```json
{"mimeType":"image/jpeg","imageBase64":"...","language":"tr"}
```
Çıktı:
```json
{
  "text":"Belgeden satır sırasına göre çıkarılan metin",
  "fields":[{"label":"Giriş saati","value":"08:10","uncertain":true}],
  "warnings":["Belirsiz alanlar fotoğrafla karşılaştırılmalı"],
  "needsReview":true
}
```

Prompt: "Belgeyi Türkçe olarak satır ve sütun sırasını koruyarak yazıya aktar. Tarih, saat, plaka, tutar, tartım ve miktarları ASLA tahmin etme. Okunamayan değeri [OKUNAMADI] yaz. Yinelenen satırları yalnızca gerçekten aynı belge parçası olduklarında birleştir. Sonucu yapılandırılmış JSON olarak döndür."

## POST /api/ask
Girdi:
```json
{"question":"Kuzey Birlik kayıtları nelerdir?","context":[{"id":1,"title":"Tartım fişi","body":"..."}]}
```
Çıktı:
```json
{"answer":"...","sourceIds":[1]}
```
Yanıtlar gönderilen kayıtlarla sınırlı kaynak göstermeli; bulunmayan veriler uydurulmamalı. Cihazdaki bütün hafıza her soruda otomatik gönderilmemeli; ilgili kayıtlar önce yerelde seçilmeli.

## Kabul testleri
1. A4 çok sütunlu malzeme listesi: satır ve sütun düzeni.
2. Tartım fişi: plaka, giriş/çıkış saatleri ve tartım değerleri; belirsiz rakamlar uyarısı.
3. Aynı fotoğrafı iki kez tarama: tek kayıt.
4. Kullanıcı onayı olmadan dışarı belge gönderilmemesi.
5. Gemini bağlantısı kesildiğinde yerel OCR'nin çalışması.
6. Kayıtların PDF/A4 olarak paylaşılması ve yazdırılması.
7. Mevcut yerel notların güncelleme sonrası korunması.

## Dağıtım önkoşulları
Sunucu adresi, kimlik doğrulama yöntemi ve güvenli Gemini API anahtarı yapılandırılmadan Gemini düğmesi çalışır olarak sunulmayacaktır.
