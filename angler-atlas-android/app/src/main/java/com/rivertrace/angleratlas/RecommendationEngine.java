package com.rivertrace.angleratlas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Transparent first-version ranking; replace weights with learned values after enough trips. */
public final class RecommendationEngine {
    public static final class Candidate {
        public final String name;
        public final double distanceKm;
        public final boolean targetFishMatched;
        public final double waterTrendFit;
        public final double historicCatchRate;

        public Candidate(String name, double distanceKm, boolean targetFishMatched,
                         double waterTrendFit, double historicCatchRate) {
            this.name = name;
            this.distanceKm = distanceKm;
            this.targetFishMatched = targetFishMatched;
            this.waterTrendFit = waterTrendFit;
            this.historicCatchRate = historicCatchRate;
        }
    }

    public static List<Candidate> rank(List<Candidate> input, double maxDistanceKm) {
        List<Candidate> result = new ArrayList<>(input);
        result.removeIf(c -> c.distanceKm > maxDistanceKm);
        result.sort(Comparator.comparingDouble(RecommendationEngine::score).reversed());
        return result;
    }

    public static double score(Candidate c) {
        double fish = c.targetFishMatched ? 1 : 0;
        double distance = Math.max(0, 1 - c.distanceKm / 60.0);
        return fish * .35 + distance * .20 + c.waterTrendFit * .20 + c.historicCatchRate * .25;
    }
}
