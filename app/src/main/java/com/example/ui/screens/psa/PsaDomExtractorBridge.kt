package com.example.ui.screens.psa

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import com.example.data.model.PsaPricingConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaPortalSyncManager
import com.example.util.PsaPriceTextParser
import org.json.JSONObject

data class PsaPriceVariation(
    val productName: String,
    val previousPrice: Double,
    val newPrice: Double,
    val difference: Double,
    val percentageChange: Double
)

/**
 * Bridge estricto bajo demanda para leer precios PVP desde el Portal DIP o Tienda Oficial.
 * Nunca sobreescribe precios automáticamente: siempre entrega candidatos verificados para confirmación del usuario.
 */
class PsaDomExtractorBridge(
    private val context: Context,
    private val currentPricing: PsaPricingConfig,
    private val countryCode: String,
    private val onCredentialsSaved: (String) -> Unit = {},
    private val onCandidatesForConfirmation: (PsaPriceTextParser.PsaParsedPriceResult) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onDipCredentialsCaptured(dipCenter: String, dipNumber: String, password: String) {
        val cleanCenter = dipCenter.trim().filter { it.isDigit() }
        val cleanDip = dipNumber.trim().filter { it.isDigit() }
        val cleanPass = password.trim()
        if (cleanDip.length < 4 || cleanPass.isEmpty()) return

        val existing = PsaPortalSyncManager.getCredentials(context)
        val hasChanged = existing.dipCenter != cleanCenter ||
            existing.dipNumber != cleanDip ||
            existing.password != cleanPass ||
            !existing.rememberCredentials

        PsaPortalSyncManager.saveCredentials(
            context,
            existing.copy(
                dipCenter = cleanCenter.ifBlank { existing.dipCenter },
                dipNumber = cleanDip,
                password = cleanPass,
                rememberCredentials = true,
                isConnected = true,
                distributorName = "Distribuidor DIP #$cleanDip",
                selectedCountryCode = countryCode
            )
        )
        try {
            CookieManager.getInstance().flush()
        } catch (_: Exception) {}

        if (hasChanged) {
            mainHandler.post {
                onCredentialsSaved(cleanDip)
            }
        }
    }

    @JavascriptInterface
    fun onStrictRowsExtracted(jsonPayload: String) {
        try {
            val root = JSONObject(jsonPayload)
            val rowsArr = root.optJSONArray("rows") ?: return
            val country = PsaCountryRegistry.getByCode(countryCode)
            val rowInputs = mutableListOf<PsaPriceTextParser.DomPriceRowInput>()

            for (i in 0 until rowsArr.length()) {
                val rObj = rowsArr.optJSONObject(i) ?: continue
                val name = rObj.optString("name", "").trim()
                val rawSnippet = rObj.optString("rawSnippet", name).trim()
                val candArr = rObj.optJSONArray("candidates") ?: continue
                val cands = mutableListOf<PsaPriceTextParser.PsaPriceCandidateOption>()
                for (j in 0 until candArr.length()) {
                    val cObj = candArr.optJSONObject(j) ?: continue
                    val rawVal = cObj.optString("raw", "")
                    val numericVal = cObj.optDouble("num", -1.0)
                    val parsed = if (numericVal > 10.0) {
                        numericVal
                    } else {
                        PsaPriceTextParser.parseLocalizedNumber(rawVal, country) ?: -1.0
                    }
                    if (parsed > 10.0) {
                        cands.add(
                            PsaPriceTextParser.PsaPriceCandidateOption(
                                price = parsed,
                                label = cObj.optString("label", "PVP").ifBlank { "PVP" },
                                isPreferredPvp = cObj.optBoolean("preferred", false)
                            )
                        )
                    }
                }
                if (name.isNotBlank() && cands.isNotEmpty()) {
                    rowInputs.add(
                        PsaPriceTextParser.DomPriceRowInput(
                            productName = name,
                            candidates = cands,
                            rawSnippet = rawSnippet
                        )
                    )
                }
            }

            val parsedFromRows = PsaPriceTextParser.parseStructuredRows(rowInputs, currentPricing, countryCode)
            val fallbackBody = root.optString("fallbackText", "")
            val parsedFromFallback = if (fallbackBody.isNotBlank()) {
                PsaPriceTextParser.parseText(fallbackBody, currentPricing, countryCode)
            } else {
                PsaPriceTextParser.PsaParsedPriceResult(emptyList())
            }

            val mergedByKey = linkedMapOf<String, PsaPriceTextParser.PsaParsedPriceItem>()
            parsedFromRows.items.forEach { mergedByKey[it.modelKey] = it }
            parsedFromFallback.items.forEach { item ->
                if (!mergedByKey.containsKey(item.modelKey)) {
                    mergedByKey[item.modelKey] = item
                }
            }
            val mergedItems = mergedByKey.values.toList()
            val finalResult = PsaPriceTextParser.PsaParsedPriceResult(
                items = mergedItems,
                detectedInstallments = parsedFromRows.detectedInstallments ?: parsedFromFallback.detectedInstallments,
                detectedCuotaSimpleFree = parsedFromRows.detectedCuotaSimpleFree || parsedFromFallback.detectedCuotaSimpleFree,
                detectedNote = if (mergedItems.isNotEmpty()) {
                    "${mergedItems.size} equipos detectados (Cocina, Baño, Aire y Pileta)"
                } else {
                    "Sin coincidencias de PVP para equipos principales"
                }
            )

            mainHandler.post {
                onCandidatesForConfirmation(finalResult)
            }
        } catch (_: Exception) {
            mainHandler.post {
                onCandidatesForConfirmation(PsaPriceTextParser.PsaParsedPriceResult(emptyList()))
            }
        }
    }

    @JavascriptInterface
    fun onRawTextForPreview(rawText: String) {
        val result = PsaPriceTextParser.parseText(rawText, currentPricing, countryCode)
        mainHandler.post {
            onCandidatesForConfirmation(result)
        }
    }

    companion object {
        private fun escapeJsString(value: String): String =
            value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")

        fun buildAutoLoginScript(dipCenter: String, dipNumber: String, password: String): String {
            val safeCenter = escapeJsString(dipCenter)
            val safeDip = escapeJsString(dipNumber)
            val safePass = escapeJsString(password)
            return """
                (function() {
                    if (window.__psaAutoLoginTried) return;
                    var numInput = document.getElementById('form_login_clavepsa_number') ||
                        document.querySelector('input[name="_number"], input[name*="number" i], input[name*="dip" i]');
                    var passInput = document.getElementById('password') ||
                        document.querySelector('input[name="_password"], input[type="password"]');
                    if (!numInput || !passInput) return 'NO_FORM';
                    window.__psaAutoLoginTried = true;

                    var centerInput = document.getElementById('form_login_clavepsa_center') ||
                        document.querySelector('input[name="_center"], input[name*="center" i]');
                    if (centerInput && '$safeCenter'.length > 0) {
                        centerInput.value = '$safeCenter';
                        centerInput.dispatchEvent(new Event('input', { bubbles: true }));
                        centerInput.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    numInput.value = '$safeDip';
                    numInput.dispatchEvent(new Event('input', { bubbles: true }));
                    numInput.dispatchEvent(new Event('change', { bubbles: true }));
                    passInput.value = '$safePass';
                    passInput.dispatchEvent(new Event('input', { bubbles: true }));
                    passInput.dispatchEvent(new Event('change', { bubbles: true }));

                    var termsCheck = document.getElementById('form_login_clavepsa_term_use') ||
                        document.querySelector('input[name="_term_use"], input[type="checkbox"]');
                    if (termsCheck && !termsCheck.checked) {
                        termsCheck.checked = true;
                        termsCheck.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    setTimeout(function() {
                        var submitBtn = document.querySelector('.js-submit-key') ||
                            document.querySelector('button[type="submit"], input[type="submit"]');
                        var form = document.getElementById('formKey') || numInput.closest('form');
                        if (submitBtn) {
                            submitBtn.click();
                        } else if (form) {
                            form.submit();
                        }
                    }, 350);
                    return 'SUBMITTED';
                })();
            """.trimIndent()
        }

        val CAPTURE_DIP_LOGIN_JS = """
            (function() {
                if (window.__psaCaptureBound) return;
                var numInput = document.getElementById('form_login_clavepsa_number') ||
                    document.querySelector('input[name="_number"], input[name*="number" i], input[name*="dip" i]');
                var passInput = document.getElementById('password') ||
                    document.querySelector('input[name="_password"], input[type="password"]');
                if (!numInput || !passInput) return;
                window.__psaCaptureBound = true;

                var centerInput = document.getElementById('form_login_clavepsa_center') ||
                    document.querySelector('input[name="_center"], input[name*="center" i]');

                function sendCapture() {
                    var cVal = centerInput ? (centerInput.value || '').trim() : '';
                    var nVal = (numInput.value || '').trim();
                    var pVal = (passInput.value || '').trim();
                    if (nVal.length >= 4 && pVal.length >= 2 && window.PsaNativeBridge && window.PsaNativeBridge.onDipCredentialsCaptured) {
                        window.PsaNativeBridge.onDipCredentialsCaptured(cVal, nVal, pVal);
                    }
                }

                var form = document.getElementById('formKey') || numInput.closest('form');
                if (form) {
                    form.addEventListener('submit', sendCapture, true);
                }
                var submitBtn = document.querySelector('.js-submit-key') ||
                    document.querySelector('button[type="submit"], input[type="submit"]');
                if (submitBtn) {
                    submitBtn.addEventListener('click', sendCapture, true);
                    submitBtn.addEventListener('pointerdown', sendCapture, true);
                }
                passInput.addEventListener('change', sendCapture);
                passInput.addEventListener('blur', sendCapture);
                numInput.addEventListener('change', sendCapture);
            })();
        """.trimIndent()

        /**
         * Extractor tabular y estricto bajo demanda para Portal DIP (Lista de Precios) y Tarjetas/Tienda Oficial.
         * - Limpia entidades HTML, caracteres especiales y superíndices de centavos.
         * - Navega filas por nombre de equipo principal (Purificadores, C3, Ducha, Pileta, Aire, Ropot).
         * - Extrae precio PVP normal de lista (prioritario) y precio de canje (opcional).
         */
        val EXTRACT_STRICT_PVP_ON_DEMAND_JS = """
            (function() {
                try {
                    var rows = [];
                    var negColRegex = /(costo|neto|\bdip\b|mayorista|distribuidor|cuota|cuotas|interes|interés|puntos|\bpv\b|\bpb\b|codigo|código|\bsku\b|litros|capacidad|dto|descuento|ahorro|bonif|margen|comisi[óo]n)/i;
                    var posColRegex = /(pvp|p[úu]blico|publico|precio\s*(?:de\s*)?lista|\blista\b|sugerido|venta\s*al\s*p[úu]blico|precio\s*p[úu]blico|precio\s*normal|valor\s*p[úu]blico)/i;
                    var canjeColRegex = /\bcanje\b/i;
                    var sparePartRegex = /\b(repuesto|recambio|fipor|n[úu]cleo|nucleo|cartucho(?:\s+de)?\s+repuesto|prefiltro|pre-?filtro|botella|bid[óo]n|bidon|jarra\s+(?:de\s+)?repuesto|kit\s+de\s+repuesto|kit\s+recambio|membrana\s+de\s+repuesto|electrodo\s+de\s+repuesto)\b/i;
                    var accessoryStartRegex = /^(?:repuesto|recambio|cartucho|prefiltro|pre-?filtro|fipor|n[úu]cleo|nucleo|botella|bid[óo]n|bidon|manguera|adaptador|v[áa]lvula|valvula|soporte|pico|vaso|electrodo|membrana|o-?ring|kit\s+)/i;
                    var equipHintRegex = /\b(senior|senik|quantum|croma|bianco|grigio|nero|vero|quanta|s[-\s]?1000|s[-\s]?3\b|mini|c3|ducha|rinnova|declorador|domus|ablandador|iontrix|pileta|piscina|air\s*purifier|purificador|descalcificador|r[-\s]?200|m[áa]xima|maxima|ropot|hidro)\b/i;
                    var currencyRegex = /(?:U\x24S|US\x24|\x24U|ARS|UYU|CLP|BOB|PYG|PEN|COP|USD|EUR|Bs\.?|S\/|€|₲|\x24)\s*:?\s*(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d{3,9}(?:[.,]\d{1,2})?)/gi;

                    function cleanSpecialText(raw) {
                        if (!raw) return '';
                        return raw
                            .replace(/[\u00A0\u200B-\u200D\uFEFF]/g, ' ')
                            .replace(/[–—]/g, '-')
                            .replace(/[^\S\r\n]+/g, ' ')
                            .replace(/ *\n */g, '\n')
                            .replace(/\n+/g, '\n')
                            .trim();
                    }

                    function isVisibleEl(el) {
                        if (!el) return false;
                        var st = window.getComputedStyle(el);
                        if (!st || st.display === 'none' || st.visibility === 'hidden' || parseFloat(st.opacity || '1') <= 0) {
                            return false;
                        }
                        var rect = el.getBoundingClientRect ? el.getBoundingClientRect() : null;
                        if (rect && rect.width === 0 && rect.height === 0) return false;
                        return true;
                    }

                    function nodeTextWithFixedSup(el) {
                        if (!el) return '';
                        var clone = el.cloneNode(true);
                        // Eliminar nodos ocultos clonados o scripts/estilos
                        var hiddenOrScripts = clone.querySelectorAll('script, style, noscript, [hidden]');
                        for (var h = 0; h < hiddenOrScripts.length; h++) {
                            if (hiddenOrScripts[h].parentNode) hiddenOrScripts[h].parentNode.removeChild(hiddenOrScripts[h]);
                        }
                        // Normalizar superíndices de centavos (ej: <sup>00</sup> -> '', <sup>50</sup> -> '.50')
                        var sups = clone.querySelectorAll('sup, .price-decimals, [class*="cents"], [class*="decimal"]');
                        for (var i = 0; i < sups.length; i++) {
                            var st = cleanSpecialText(sups[i].textContent || '').replace(/[^\d]/g, '');
                            if (/^\d{2}$/.test(st)) {
                                sups[i].textContent = (st === '00') ? '' : ('.' + st);
                            }
                        }
                        var blocks = clone.querySelectorAll('div, p, li, tr, td, th, h1, h2, h3, h4, h5, h6, br, section, article');
                        for (var b = 0; b < blocks.length; b++) {
                            if (blocks[b].parentNode) {
                                blocks[b].parentNode.insertBefore(document.createTextNode('\n'), blocks[b].nextSibling);
                            }
                        }
                        return cleanSpecialText(clone.textContent || '');
                    }

                    function extractAmountsFromText(text, forceAllowBare, label, preferred) {
                        var out = [];
                        if (!text) return out;
                        var normalized = cleanSpecialText(text);
                        var m;
                        currencyRegex.lastIndex = 0;
                        while ((m = currencyRegex.exec(normalized)) !== null) {
                            var start = Math.max(0, m.index - 24);
                            var before = normalized.substring(start, m.index);
                            var after = normalized.substring(m.index + m[0].length, m.index + m[0].length + 20);
                            var isCanjeLocal = canjeColRegex.test(before);
                            if (!preferred && !isCanjeLocal && (/(cuotas?\s+de|x\s*$|c\/u|costo|neto|\bdip\b|puntos|\bpv\b|\bpb\b|dto|descuento)/i.test(before) || /^\s*(litros?|lts?\b|cuotas?|%)/i.test(after))) {
                                continue;
                            }
                            out.push({
                                raw: m[1],
                                label: isCanjeLocal ? 'Canje' : label,
                                preferred: isCanjeLocal ? false : preferred
                            });
                        }
                        if (out.length === 0 && forceAllowBare) {
                            var bare = normalized.match(/(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d{4,9}(?:[.,]\d{1,2})?)/);
                            if (bare && bare[1] && !/^\s*(litros?|lts?\b|cuotas?|%|\bpv\b|\bpb\b)/i.test(normalized)) {
                                out.push({ raw: bare[1], label: label, preferred: preferred });
                            }
                        }
                        return out;
                    }

                    function isSparePartName(name) {
                        var cleaned = cleanSpecialText(name).replace(/\+?\s*kit\s*posv\.?/gi, ' ').trim();
                        if (!cleaned) return true;
                        var hasMainEquip = equipHintRegex.test(cleaned) || /\b(purificador|declorador|ablandador|descalcificador)\b/i.test(cleaned);
                        if (sparePartRegex.test(cleaned)) return true;
                        if (!hasMainEquip && accessoryStartRegex.test(cleaned)) return true;
                        return /^kit\s+(?:posv|de\s+repuesto|recambio)/i.test(cleaned) && !hasMainEquip;
                    }

                    // 1. Tablas HTML visibles (Portal DIP Lista de Precios)
                    document.querySelectorAll('table').forEach(function(table) {
                        if (!isVisibleEl(table)) return;
                        var headers = [];
                        var headerRows = table.querySelectorAll('thead tr');
                        var firstRow = headerRows.length > 0 ? headerRows[headerRows.length - 1] : table.querySelector('tr');
                        if (firstRow) {
                            firstRow.querySelectorAll('th, td').forEach(function(cell) {
                                headers.push(nodeTextWithFixedSup(cell).replace(/\n/g, ' ').trim());
                            });
                        }
                        var bodyRows = table.querySelectorAll('tbody tr');
                        if (bodyRows.length === 0) bodyRows = table.querySelectorAll('tr');

                        bodyRows.forEach(function(tr, idx) {
                            if (!isVisibleEl(tr)) return;
                            if (tr.closest && tr.closest('thead')) return;
                            if (idx === 0 && tr.querySelectorAll('th').length > 0) return;
                            var cells = tr.querySelectorAll('td, th');
                            if (cells.length < 2) return;

                            var explicitEquipName = '';
                            var fallbackName = '';
                            var cands = [];
                            cells.forEach(function(td, colIdx) {
                                var txt = nodeTextWithFixedSup(td).replace(/\n/g, ' ').trim();
                                if (!txt) return;
                                var hdr = headers[colIdx] || '';
                                var isCodeOrMetaCol = /(c[óo]digo|cod\.|\bsku\b|puntos|\bpv\b|\bpb\b|cant|unidad)/i.test(hdr);
                                var isNegCol = negColRegex.test(hdr);
                                var isCanjeCol = canjeColRegex.test(hdr);
                                var isPosCol = posColRegex.test(hdr) || (!isNegCol && !isCanjeCol && /\b(precio|valor|importe|venta|contado)\b/i.test(hdr));

                                // Identificar celda con nombre de equipo sin quedar atrapado en SKU de columna 0
                                var looksLikeSku = /^[A-Z]{1,5}[-_/]?\d{2,7}$/i.test(txt) || /^\d+$/.test(txt);
                                var looksLikePriceOnly = /^[\x24\s.,\d]+$/.test(txt) || hasCurrencyRegex.test(txt);
                                if (!isCodeOrMetaCol && !looksLikeSku && !looksLikePriceOnly && /[a-zA-Z]{2,}/.test(txt)) {
                                    if (equipHintRegex.test(txt) || /\bpsa\b/i.test(txt)) {
                                        if (!explicitEquipName || txt.length > explicitEquipName.length) explicitEquipName = txt;
                                    } else if (txt.length >= 3 && !/^(si|no|ver|ver\s+m[áa]s)$/i.test(txt)) {
                                        if (!fallbackName || txt.length > fallbackName.length) fallbackName = txt;
                                    }
                                }

                                if (!isNegCol) {
                                    var colLabel = isCanjeCol ? 'Canje' : (isPosCol ? 'PVP Lista' : 'PVP Normal');
                                    var isPref = !isCanjeCol && (isPosCol || /\b(lista|pvp|p[úu]blico)\b/i.test(txt));
                                    var found = extractAmountsFromText(txt, isPosCol || isCanjeCol, colLabel, isPref);
                                    found.forEach(function(f) {
                                        if (isCanjeCol) {
                                            f.label = 'Canje';
                                            f.preferred = false;
                                        }
                                        cands.push(f);
                                    });
                                }
                            });

                            var rowName = explicitEquipName || fallbackName;
                            if (rowName && !isSparePartName(rowName) && cands.length > 0) {
                                rows.push({
                                    name: rowName,
                                    candidates: cands,
                                    rawSnippet: rowName + ' | ' + cands.map(function(c){ return c.label + ':' + c.raw; }).join(' ')
                                });
                            }
                        });
                    });

                    // 2. Tablas responsive con role="table" / grid del Portal DIP
                    document.querySelectorAll('[role="table"], .price-list, .lista-precios, .listado-precios').forEach(function(tbl) {
                        if (!isVisibleEl(tbl)) return;
                        tbl.querySelectorAll('[role="row"], .price-row, .row, .item-row').forEach(function(rEl) {
                            if (!isVisibleEl(rEl)) return;
                            var txt = nodeTextWithFixedSup(rEl).replace(/\n/g, ' ').trim();
                            if (!txt || txt.length < 8 || txt.length > 320 || isSparePartName(txt)) return;
                            if (!equipHintRegex.test(txt) && !/\bpsa\b/i.test(txt)) return;
                            var isPos = posColRegex.test(txt);
                            var cands = extractAmountsFromText(txt, false, isPos ? 'PVP Lista' : 'PVP Normal', true);
                            if (cands.length > 0) {
                                rows.push({ name: txt, candidates: cands, rawSnippet: txt });
                            }
                        });
                    });

                    // 3. Tarjetas de tienda oficial (oldPrice PVP vs finalPrice)
                    document.querySelectorAll('.product-item, .product-item-info, .product-card').forEach(function(card) {
                        if (!isVisibleEl(card)) return;
                        var titleEl = card.querySelector('.product-item-link, .product-item-name, h2, h3');
                        if (!titleEl) return;
                        var name = nodeTextWithFixedSup(titleEl).replace(/\n/g, ' ').trim();
                        if (!name || isSparePartName(name)) return;
                        var cands = [];
                        var oldEl = card.querySelector('[data-price-type="oldPrice"][data-price-amount], .old-price [data-price-amount]');
                        if (oldEl) {
                            var oldAmt = parseFloat(oldEl.getAttribute('data-price-amount'));
                            if (!isNaN(oldAmt) && oldAmt > 10) cands.push({ num: oldAmt, label: 'PVP Lista', preferred: true });
                        }
                        var finalEl = card.querySelector('[data-price-type="finalPrice"][data-price-amount]');
                        if (finalEl) {
                            var finAmt = parseFloat(finalEl.getAttribute('data-price-amount'));
                            if (!isNaN(finAmt) && finAmt > 10) cands.push({ num: finAmt, label: cands.length > 0 ? 'Promo Online' : 'PVP Normal', preferred: cands.length === 0 });
                        }
                        if (cands.length > 0) {
                            rows.push({ name: name, candidates: cands, rawSnippet: name });
                        }
                    });

                    // 4. Vista detalle de producto (.product-info-main)
                    document.querySelectorAll('.product-info-main').forEach(function(detail) {
                        if (!isVisibleEl(detail)) return;
                        var titleEl = detail.querySelector('.page-title, h1');
                        if (!titleEl) return;
                        var name = nodeTextWithFixedSup(titleEl).replace(/\n/g, ' ').trim();
                        if (!name || isSparePartName(name)) return;
                        var cands = [];
                        var oldEl = detail.querySelector('.product-info-price [data-price-type="oldPrice"][data-price-amount]');
                        if (oldEl) {
                            var oldAmt = parseFloat(oldEl.getAttribute('data-price-amount'));
                            if (!isNaN(oldAmt) && oldAmt > 10) cands.push({ num: oldAmt, label: 'PVP Lista', preferred: true });
                        }
                        var finalEl = detail.querySelector('.product-info-price [data-price-type="finalPrice"][data-price-amount]');
                        if (finalEl) {
                            var finAmt = parseFloat(finalEl.getAttribute('data-price-amount'));
                            if (!isNaN(finAmt) && finAmt > 10) cands.push({ num: finAmt, label: cands.length > 0 ? 'Promo Online' : 'PVP Normal', preferred: cands.length === 0 });
                        }
                        if (cands.length > 0) {
                            rows.push({ name: name, candidates: cands, rawSnippet: name });
                        }
                    });

                    // 5. Tarjetas del Portal DIP (ej. mi.psa.com.ar: "+ KIT POSV.", "Pb a tu cuenta", "canje:")
                    var hasCurrencyRegex = /(\x24|ARS|UYU|CLP|BOB|PYG|PEN|COP|USD|EUR|Bs\.?|S\/)\s*\d/i;
                    var titleHintRegex = /^(?:purificador|declorador|ablandador|descalcificador|psa|ducha|rinnova|domus|senior|senik|quantum|croma|vero|quanta|mini|c3|iontrix|air\s*purifier|ropot|m[áa]xima|maxima|s[-\s]?1000|s[-\s]?3)\b/i;
                    var noiseBadgeRegex = /^(?:nuevo|destacado|stock|disponible|en\s+stock|sin\s+stock|ver\s+detalle|ver\s+m[áa]s|l[íi]nea\s+\w+|cocina|ba[ñn]o|hogar|pileta|aire|entrada)$/i;
                    var seenCardTitles = {};

                    function getTitleCandidates(lines, isDipTemplate) {
                        return lines.filter(function(l) {
                            if (noiseBadgeRegex.test(l)) return false;
                            if ((titleHintRegex.test(l) || equipHintRegex.test(l)) && !/(pb\s+a\s+tu\s+cuenta|planes\s+de\s+pago)/i.test(l)) return true;
                            if (isDipTemplate && l.length >= 4 && l.length <= 95 &&
                                !/^[+\x24\d]/.test(l) &&
                                !/(pb\s+a\s+tu\s+cuenta|planes\s+de\s+pago|^\s*canje|cuotas?\s+de)/i.test(l) &&
                                !hasCurrencyRegex.test(l)) {
                                return true;
                            }
                            return false;
                        });
                    }

                    function isSingleCardNode(node) {
                        if (!isVisibleEl(node)) return null;
                        if (node.closest && node.closest('header, nav, footer, table')) return null;
                        var text = nodeTextWithFixedSup(node);
                        if (!text || text.length < 12 || text.length > 820) return null;
                        if (!hasCurrencyRegex.test(text)) return null;

                        var isDipTemplate = /(pb\s+a\s+tu\s+cuenta|planes\s+de\s+pago|\bcanje\s*:)/i.test(text);
                        var hasProductWord = /\bpsa\b/i.test(text) || equipHintRegex.test(text) || titleHintRegex.test(text);
                        if (!isDipTemplate && !hasProductWord) return null;

                        var lines = text.split('\n').map(function(l) { return l.trim(); }).filter(function(l) { return l.length > 0; });
                        if (lines.length === 0 || lines.length > 24) return null;

                        var titleCandidates = getTitleCandidates(lines, isDipTemplate);
                        if (titleCandidates.length === 0) return null;
                        var strongMatches = titleCandidates.filter(function(c) { return titleHintRegex.test(c) || /\bpsa\b/i.test(c); });
                        if (strongMatches.length > 2) return null;
                        var chosenTitle = strongMatches.length > 0 ? strongMatches[0] : titleCandidates[0];
                        if (isSparePartName(chosenTitle)) return null;
                        return { text: text, lines: lines, title: chosenTitle };
                    }

                    document.querySelectorAll('article, li, div, section, a').forEach(function(el) {
                        var cardInfo = isSingleCardNode(el);
                        if (!cardInfo) return;

                        var hasTighterCardDescendant = false;
                        var childCards = el.querySelectorAll('article, li, div, section, a');
                        for (var c = 0; c < childCards.length; c++) {
                            if (isSingleCardNode(childCards[c]) !== null) {
                                hasTighterCardDescendant = true;
                                break;
                            }
                        }
                        if (hasTighterCardDescendant) return;

                        var productTitle = cardInfo.title;
                        var cands = [];
                        cardInfo.lines.forEach(function(line) {
                            if (line === productTitle) return;
                            if (/(cuotas?\s+de|planes\s+de\s+pago|pb\s+a\s+tu\s+cuenta|pb\s+por\s+canje)/i.test(line)) return;
                            var isCanjeLine = canjeColRegex.test(line);
                            var found = extractAmountsFromText(
                                line,
                                false,
                                isCanjeLine ? 'Canje' : 'PVP Normal',
                                !isCanjeLine
                            );
                            found.forEach(function(f) {
                                if (isCanjeLine) {
                                    f.label = 'Canje';
                                    f.preferred = false;
                                }
                                cands.push(f);
                            });
                        });

                        if (cands.length > 0) {
                            var dedupeKey = productTitle.toUpperCase() + '|' + cands.map(function(x){ return x.raw; }).join(',');
                            if (!seenCardTitles[dedupeKey]) {
                                seenCardTitles[dedupeKey] = true;
                                rows.push({ name: productTitle, candidates: cands, rawSnippet: cardInfo.text });
                            }
                        }
                    });

                    var fallbackText = document.body ? nodeTextWithFixedSup(document.body) : '';
                    if (window.PsaNativeBridge && window.PsaNativeBridge.onStrictRowsExtracted) {
                        window.PsaNativeBridge.onStrictRowsExtracted(JSON.stringify({
                            rows: rows,
                            sourceUrl: window.location ? (window.location.href || '') : '',
                            fallbackText: fallbackText
                        }));
                    }
                } catch(e) {
                    if (window.PsaNativeBridge && window.PsaNativeBridge.onRawTextForPreview) {
                        window.PsaNativeBridge.onRawTextForPreview(document.body ? (document.body.innerText || '') : '');
                    }
                }
            })();
        """
    }
}
