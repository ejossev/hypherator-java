package io.sevcik.hypherator;

import io.sevcik.hypherator.dto.PotentialBreak;

record PotentialBreakImpl(int position, int priority, HyphenDict.BreakRule breakRule)  implements PotentialBreak {}
