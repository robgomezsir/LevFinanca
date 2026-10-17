package com.example.lev.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Durações e easing padrão da Fase 2. Regra: animação não pode atrasar a tarefa —
 * nada acima de 300ms além de navegação. Componentes must respect reduced motion
 * (o padrão já presente em LevProgressBar / AnimatedMoneyText deve se espalhar).
 */
object LevMotion {
    /** Feedback de toque / troca de estado local (ripple, chip selection). */
    const val DurationFast = 120

    /** Transição de valor: progresso, badges, expansão de card. */
    const val DurationBase = 200

    /** Transição de navegação / bottom sheet inteira. */
    const val DurationSlow = 300

    /** Curva padrão (material standard easing). */
    val EasingStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Curva de desaceleração (elemento entra na tela). */
    val EasingDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}
