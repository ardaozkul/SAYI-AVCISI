package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import com.sayiavcisi.app.model.QuestionFormat
import com.sayiavcisi.app.model.TargetStep
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/**
 * Sayı Avcısı Eşleştirme ve Matematiksel Doğrulama Motoru.
 *
 * Kurallar:
 * - Yatay sağa ve sola
 * - Dikey yukarı ve aşağı
 * - Çapraz tüm yönlerde (8 yön)
 * - Ters yönde okuma
 * - Birbirine bitişik olmayan sayılar (aradaki bloklar temizlenmişse)
 * - Aralarında başka bir aktif sayı bulunmayan, aynı doğrultudaki sayılar
 * - Serbest eşleştirme modu ayrı bir oyun kuralı olarak sunulur
 */
object MatchingValidator {

    /**
     * İki hücrenin (a ve b) aynı doğrultuda (yatay, dikey veya 45 derece çapraz) olup olmadığını
     * ve aralarında başka hiçbir aktif (isCleared == false) sayı bloğu bulunmadığını kontrol eder.
     */
    fun isClearLineOfSight(
        a: Pair<Int, Int>,
        b: Pair<Int, Int>,
        activeCoords: Set<Pair<Int, Int>>
    ): Boolean {
        if (a == b) return false
        val dr = b.first - a.first
        val dc = b.second - a.second

        val isHorizontal = dr == 0 && dc != 0
        val isVertical = dc == 0 && dr != 0
        val isDiagonal = abs(dr) == abs(dc) && dr != 0

        if (!isHorizontal && !isVertical && !isDiagonal) {
            return false
        }

        val stepR = dr.sign
        val stepC = dc.sign
        val steps = max(abs(dr), abs(dc))

        for (i in 1 until steps) {
            val mid = Pair(a.first + i * stepR, a.second + i * stepC)
            if (activeCoords.contains(mid)) {
                // Aralarında başka bir aktif sayı bloğu var!
                return false
            }
        }
        return true
    }

    /**
     * Seçilen hücre koordinatlarının, verilen eşleştirme moduna (DIRECTIONAL veya FREE)
     * göre geometrik olarak geçerli bir dizilim oluşturup oluşturmadığını doğrular.
     *
     * DIRECTIONAL modda:
     * - 2 hücre için: Aynı yatay, dikey veya çapraz doğrultuda olmalı ve aralarında aktif blok olmamalı.
     *   (Hem ileri hem ters yönde seçime izin verir.)
     * - 3 hücre için: Üç hücre aynı doğrultuda (satır, sütun veya çapraz hat) yer almalı ve
     *   bu hat üzerindeki sıralı uçlar arasında seçilmeyen başka bir aktif blok bulunmamalı.
     *
     * FREE modda:
     * - Seçilen hücreler birbirinden farklı olduğu sürece herhangi bir satır/sütundan serbestçe seçilebilir.
     */
    fun isGeometricallyValidSelection(
        selectedCoords: List<Pair<Int, Int>>,
        activeCoords: Set<Pair<Int, Int>>,
        mode: MatchRuleMode
    ): Pair<Boolean, String?> {
        if (selectedCoords.size < 2) {
            return Pair(false, "En az iki sayı seçilmelidir.")
        }
        if (selectedCoords.distinct().size != selectedCoords.size) {
            return Pair(false, "Aynı sayı bloğu birden fazla kez seçilemez.")
        }
        if (!selectedCoords.all { activeCoords.contains(it) }) {
            return Pair(false, "Seçilen bloklardan biri zaten temizlenmiş.")
        }

        if (mode == MatchRuleMode.FREE) {
            return Pair(true, null)
        }

        if (selectedCoords.size == 2) {
            val clear = isClearLineOfSight(selectedCoords[0], selectedCoords[1], activeCoords)
            return if (clear) {
                Pair(true, null)
            } else {
                val a = selectedCoords[0]
                val b = selectedCoords[1]
                val dr = b.first - a.first
                val dc = b.second - a.second
                val collinear = (dr == 0 || dc == 0 || abs(dr) == abs(dc))
                if (!collinear) {
                    Pair(false, "Sayılar aynı yatay, dikey veya çapraz doğrultuda olmalı!")
                } else {
                    Pair(false, "Seçtiğin iki sayının arasında başka bir sayı bloğu var! Önce aradaki bloğu yok etmelisin.")
                }
            }
        }

        if (selectedCoords.size == 3) {
            // 3 hücrenin aynı doğrultuda olup olmadığını kontrol et (sıralamadan bağımsız olarak hat üzerinde mi)
            val sorted = selectedCoords.sortedWith(compareBy({ it.first }, { it.second }))
            val p0 = sorted[0]
            val p1 = sorted[1]
            val p2 = sorted[2]

            val dr1 = p1.first - p0.first
            val dc1 = p1.second - p0.second
            val dr2 = p2.first - p1.first
            val dc2 = p2.second - p1.second

            val validDir1 = (dr1 == 0 && dc1 != 0) || (dc1 == 0 && dr1 != 0) || (abs(dr1) == abs(dc1) && dr1 != 0)
            val sameDirection = validDir1 && (dr1.sign == dr2.sign) && (dc1.sign == dc2.sign)

            if (!sameDirection) {
                return Pair(false, "Üç sayı da aynı yatay, dikey veya çapraz doğrultuda bulunmalı!")
            }

            // Ardışık çiftler arasında başka aktif blok olmamalı
            val clear01 = isClearLineOfSight(p0, p1, activeCoords)
            val clear12 = isClearLineOfSight(p1, p2, activeCoords)
            return if (clear01 && clear12) {
                Pair(true, null)
            } else {
                Pair(false, "Seçtiğin üç sayının arasında başka bir sayı bloğu bulunmamalı!")
            }
        }

        return Pair(false, "Desteklenmeyen seçim sayısı.")
    }

    /**
     * Seçilen sayı değerlerinin, aktif hedefe (TargetStep) matematiksel olarak uyup uymadığını kontrol eder.
     *
     * ÖNEMLİ KURALLAR:
     * - Toplama: 2 veya 3 sayının toplamı hedefe eşit olmalı (sıra önemsiz).
     * - Çıkarma:
     *   - 2 sayı: Büyük sayıdan küçük sayı çıkarılması zorunlu! Çocuk ilk olarak büyük sayıyı,
     *     ikinci olarak küçük sayıyı seçmeli (selectedValues[0] >= selectedValues[1]) ve farkları hedefe eşit olmalı.
     *   - 3 sayı (Zincir çıkarma): İlk sayıdan diğer iki sayı sırayla çıkarıldığında (a - b - c = hedef)
     *     negatif olmayan ara sonuç ve hedef elde edilmeli (a >= b ve a - b >= c).
     * - Çarpma:
     *   - 2 sayı çarpımı hedefe eşit olmalı.
     *   - Eksik çarpan modunda (? × K = Hedef): Seçilen iki sayıdan biri K, diğeri eksik çarpan olmalı ve çarpımları hedefi vermeli.
     * - Bölme:
     *   - Sıfıra bölme kesinlikle yasaktır (bölen != 0).
     *   - Kalansız bölme zorunludur (bölünen % bölen == 0).
     *   - Çocuk büyük sayıyı (bölünen) seçip bölen sayıyı bulmalı veya bölme işlemine uygun iki sayıyı eşleştirmeli.
     *   - Eksik bölen (A ÷ ? = Hedef) veya eksik bölünen (? ÷ B = Hedef) modlarında ilgili sabit değer seçilen çiftte yer almalıdır.
     */
    fun evaluateMathMatch(
        selectedValues: List<Int>,
        step: TargetStep
    ): MathEvaluationResult {
        if (selectedValues.size != step.operandCount) {
            return MathEvaluationResult(
                isValid = false,
                expression = selectedValues.joinToString(" ? "),
                resultValue = 0,
                feedbackMessage = "Bu hedef için ${step.operandCount} sayı seçmelisin."
            )
        }

        return when (step.operation) {
            OperationType.ADDITION -> {
                val sum = selectedValues.sum()
                val expr = "${selectedValues.joinToString(" + ")} = $sum"
                if (sum == step.targetResult) {
                    val zeroNote = if (selectedValues.contains(0)) {
                        " Harika! 0 etkisiz elemandır, toplamı değiştirmez."
                    } else ""
                    MathEvaluationResult(
                        isValid = true,
                        expression = expr,
                        resultValue = sum,
                        feedbackMessage = "Tebrikler! $expr doğru eşleşme!$zeroNote"
                    )
                } else {
                    MathEvaluationResult(
                        isValid = false,
                        expression = expr,
                        resultValue = sum,
                        feedbackMessage = "Seçtiğin sayıların toplamı $sum yapıyor, ama hedefimiz ${step.targetResult}. Tekrar dene!"
                    )
                }
            }

            OperationType.SUBTRACTION -> {
                if (selectedValues.size == 2) {
                    val first = selectedValues[0]
                    val second = selectedValues[1]
                    if (first < second) {
                        return MathEvaluationResult(
                            isValid = false,
                            expression = "$first - $second",
                            resultValue = first - second,
                            feedbackMessage = "Çıkarma işleminde önce BÜYÜK sayıyı ($second), sonra küçük sayıyı ($first) seçmelisin!"
                        )
                    }
                    val diff = first - second
                    val expr = "$first - $second = $diff"
                    if (diff == step.targetResult) {
                        MathEvaluationResult(
                            isValid = true,
                            expression = expr,
                            resultValue = diff,
                            feedbackMessage = "Harika! $first nesneden $second tanesi eksildi ve $diff kaldı!"
                        )
                    } else {
                        MathEvaluationResult(
                            isValid = false,
                            expression = expr,
                            resultValue = diff,
                            feedbackMessage = "$expr sonucu çıkıyor, fakat hedefimiz ${step.targetResult}."
                        )
                    }
                } else {
                    // 3 sayılı zincir çıkarma: a - b - c = hedef
                    val a = selectedValues[0]
                    val b = selectedValues[1]
                    val c = selectedValues[2]
                    if (a < b || (a - b) < c) {
                        return MathEvaluationResult(
                            isValid = false,
                            expression = "$a - $b - $c",
                            resultValue = a - b - c,
                            feedbackMessage = "Zincir çıkarmada önce en büyük sayıyı seçmeli ve sonucun sıfırdan küçük olmamasına dikkat etmelisin!"
                        )
                    }
                    val diff = a - b - c
                    val expr = "$a - $b - $c = $diff"
                    if (diff == step.targetResult) {
                        MathEvaluationResult(
                            isValid = true,
                            expression = expr,
                            resultValue = diff,
                            feedbackMessage = "Süper zincir çıkarma! $expr"
                        )
                    } else {
                        MathEvaluationResult(
                            isValid = false,
                            expression = expr,
                            resultValue = diff,
                            feedbackMessage = "$expr çıkıyor, ama hedefimiz ${step.targetResult}."
                        )
                    }
                }
            }

            OperationType.MULTIPLICATION -> {
                val a = selectedValues[0]
                val b = selectedValues[1]
                val prod = a * b
                val expr = "$a × $b = $prod"

                if (step.questionFormat == QuestionFormat.MISSING_FACTOR && step.fixedKnownOperand != null) {
                    val known = step.fixedKnownOperand
                    if (a != known && b != known) {
                        return MathEvaluationResult(
                            isValid = false,
                            expression = expr,
                            resultValue = prod,
                            feedbackMessage = "İpucu: Sorudaki '$known' çarpanını ve eksik çarpanı birlikte seçmelisin! (? × $known = ${step.targetResult})"
                        )
                    }
                }

                if (prod == step.targetResult) {
                    val explain = when {
                        a == 0 || b == 0 -> "Sıfır yutan elemandır! Hangi sayıyı 0 ile çarparsan sonuç 0 olur."
                        a == 1 || b == 1 -> "1 etkisiz çarpan! Sayının kendisini verir."
                        else -> "$a tane $b grubunun toplamı $prod eder!"
                    }
                    MathEvaluationResult(
                        isValid = true,
                        expression = expr,
                        resultValue = prod,
                        feedbackMessage = "Mükemmel! $expr. $explain"
                    )
                } else {
                    MathEvaluationResult(
                        isValid = false,
                        expression = expr,
                        resultValue = prod,
                        feedbackMessage = "Görsel İpucu: $a tane $b yan yana gelirse $prod olur. Hedefimiz ${step.targetResult} için başka iki çarpan dene!"
                    )
                }
            }

            OperationType.DIVISION -> {
                val first = selectedValues[0]
                val second = selectedValues[1]

                // Sıfıra bölme kontrolü
                if (second == 0) {
                    return MathEvaluationResult(
                        isValid = false,
                        expression = "$first ÷ 0",
                        resultValue = 0,
                        feedbackMessage = "Bir sayıyı 0'a bölemeyiz! Önce bölünen sayıyı, sonra 0 olmayan bölen sayıyı seçmelisin."
                    )
                }

                // Çocuk büyük sayıyı (bölünen) önce seçtiyse veya ters sırada seçtiyse:
                // Bölünen = first, bölen = second olarak önceliklendir, ancak first < second ve first != 0 ise uyar veya kontrol et
                val dividend: Int
                val divisor: Int
                if (first >= second || first == 0) {
                    dividend = first
                    divisor = second
                } else {
                    // Ters sırada seçilmişse ancak kalansız bölünüyorsa (ör. 2 ve 6 seçildi, 6 ÷ 2 = 3)
                    if (first != 0 && second % first == 0) {
                        dividend = second
                        divisor = first
                    } else {
                        return MathEvaluationResult(
                            isValid = false,
                            expression = "$first ÷ $second",
                            resultValue = 0,
                            feedbackMessage = "Bölme işleminde büyük sayıyı (bölünen) küçük sayıya (bölen) kalansız bölmeliyiz!"
                        )
                    }
                }

                if (divisor == 0) {
                    return MathEvaluationResult(
                        isValid = false,
                        expression = "$dividend ÷ 0",
                        resultValue = 0,
                        feedbackMessage = "Sıfıra bölme yapılamaz!"
                    )
                }

                if (dividend % divisor != 0) {
                    return MathEvaluationResult(
                        isValid = false,
                        expression = "$dividend ÷ $divisor",
                        resultValue = 0,
                        feedbackMessage = "$dividend sayısı $divisor sayısına kalansız bölünmüyor! Çarpım tablosunu düşün: ? × $divisor = $dividend"
                    )
                }

                val quotient = dividend / divisor
                val expr = "$dividend ÷ $divisor = $quotient"

                if (step.questionFormat == QuestionFormat.MISSING_DIVISOR && step.fixedKnownOperand != null) {
                    if (dividend != step.fixedKnownOperand) {
                        return MathEvaluationResult(
                            isValid = false,
                            expression = expr,
                            resultValue = quotient,
                            feedbackMessage = "Soruda bölünen sayı ${step.fixedKnownOperand} olarak verilmiş (${step.fixedKnownOperand} ÷ ? = ${step.targetResult}). ${step.fixedKnownOperand} ile doğru böleni eşleştir!"
                        )
                    }
                } else if (step.questionFormat == QuestionFormat.MISSING_DIVIDEND && step.fixedKnownOperand != null) {
                    if (divisor != step.fixedKnownOperand) {
                        return MathEvaluationResult(
                            isValid = false,
                            expression = expr,
                            resultValue = quotient,
                            feedbackMessage = "Soruda bölen sayı ${step.fixedKnownOperand} olarak verilmiş (? ÷ ${step.fixedKnownOperand} = ${step.targetResult}). Doğru bölünen ile ${step.fixedKnownOperand} sayısını eşleştir!"
                        )
                    }
                }

                if (quotient == step.targetResult) {
                    MathEvaluationResult(
                        isValid = true,
                        expression = expr,
                        resultValue = quotient,
                        feedbackMessage = "Harika! $expr (Çünkü $quotient × $divisor = $dividend)"
                    )
                } else {
                    MathEvaluationResult(
                        isValid = false,
                        expression = expr,
                        resultValue = quotient,
                        feedbackMessage = "$expr sonucu $quotient çıkıyor, ama hedefimiz ${step.targetResult}!"
                    )
                }
            }

            OperationType.MIXED -> {
                // TargetStep her zaman somut bir işlem (ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION) taşır.
                MathEvaluationResult(false, "", 0, "Geçersiz işlem türü")
            }
        }
    }

    /**
     * Mevcut oyun tahtasında verilen TargetStep için hem geometrik hem de matematiksel olarak
     * geçerli olan tüm hamleleri (koordinat listesi olarak) bulur.
     *
     * Bu fonksiyon:
     * 1. Tahta üretildiğinde çözülebilirlik doğrulamasında,
     * 2. Her hamle sonrasında tahtanın kilitlenip kilitlenmediğini anlamada,
     * 3. İPUCU jokerinde,
     * 4. ALTIN KURTARMA sisteminde kullanılır.
     */
    fun findAllValidMovesForStep(
        cells: List<BlockCell>,
        step: TargetStep,
        mode: MatchRuleMode
    ): List<List<Pair<Int, Int>>> {
        val activeCells = cells.filter { !it.isCleared }
        val activeCoords = activeCells.map { Pair(it.row, it.col) }.toSet()
        val cellByCoord = activeCells.associateBy { Pair(it.row, it.col) }
        val validMoves = mutableListOf<List<Pair<Int, Int>>>()

        if (step.operandCount == 2) {
            for (i in activeCells.indices) {
                for (j in activeCells.indices) {
                    if (i == j) continue
                    val c1 = activeCells[i]
                    val c2 = activeCells[j]
                    val coords = listOf(Pair(c1.row, c1.col), Pair(c2.row, c2.col))
                    val (geoOk, _) = isGeometricallyValidSelection(coords, activeCoords, mode)
                    if (!geoOk) continue

                    val mathRes = evaluateMathMatch(listOf(c1.value, c2.value), step)
                    if (mathRes.isValid) {
                        validMoves.add(coords)
                    }
                }
            }
        } else if (step.operandCount == 3) {
            val n = activeCells.size
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (j == i) continue
                    for (k in 0 until n) {
                        if (k == i || k == j) continue
                        val c1 = activeCells[i]
                        val c2 = activeCells[j]
                        val c3 = activeCells[k]
                        val coords = listOf(
                            Pair(c1.row, c1.col),
                            Pair(c2.row, c2.col),
                            Pair(c3.row, c3.col)
                        )
                        val (geoOk, _) = isGeometricallyValidSelection(coords, activeCoords, mode)
                        if (!geoOk) continue

                        val mathRes = evaluateMathMatch(listOf(c1.value, c2.value, c3.value), step)
                        if (mathRes.isValid) {
                            validMoves.add(coords)
                        }
                    }
                }
            }
        }

        // Aynı hücre kümesini tekrarlayan permütasyonları sadeleştir
        return validMoves.distinctBy { move ->
            if (step.operation == OperationType.SUBTRACTION || step.operation == OperationType.DIVISION) {
                move // Sıralama önemli
            } else {
                move.sortedWith(compareBy({ it.first }, { it.second }))
            }
        }
    }
}

data class MathEvaluationResult(
    val isValid: Boolean,
    val expression: String,
    val resultValue: Int,
    val feedbackMessage: String
)
