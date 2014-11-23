package de.glmtk.querying.estimator.discount;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.glmtk.Constants;
import de.glmtk.querying.CountCache;
import de.glmtk.querying.estimator.fraction.FractionEstimator;
import de.glmtk.utils.NGram;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.PatternElem;

public class ModifiedKneserNeyDiscountEstimator extends DiscountEstimator {

    private Map<Pattern, Double> discount1 = null;

    private Map<Pattern, Double> discount2 = null;

    private Map<Pattern, Double> discount3p = null;

    public ModifiedKneserNeyDiscountEstimator(
            FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);

        // TODO calc discount
        discount1 = new HashMap<Pattern, Double>();
        discount2 = new HashMap<Pattern, Double>();
        discount3p = new HashMap<Pattern, Double>();
        for (Pattern pattern : Pattern.getCombinations(Constants.MODEL_SIZE,
                Arrays.asList(PatternElem.CNT, PatternElem.SKP))) {
            double n1 = countCache.getNGramTimesCount(pattern, 1);
            double n2 = countCache.getNGramTimesCount(pattern, 2);
            double n3 = countCache.getNGramTimesCount(pattern, 3);
            double n4 = countCache.getNGramTimesCount(pattern, 4);
            double y = n1 / (n1 + n2);
            discount1.put(pattern, 1 - 2 * y * n2 / n1);
            discount2.put(pattern, 2 - 3 * y * n3 / n2);
            discount3p.put(pattern, 3 - 4 * y * n4 / n3);
        }
    }

    @Override
    protected double calcDiscount(NGram sequence, NGram history, int recDepth) {
        switch ((int) countCache.getAbsolute(history)) {
            case 0:
                return 0;
            case 1:
                return discount1.get(history.toPattern());
            case 2:
                return discount2.get(history.toPattern());
            default:
                return discount3p.get(history.toPattern());
        }
    }

    public double getDiscount1(Pattern pattern) {
        return discount1.get(pattern);
    }

    public double getDiscount2(Pattern pattern) {
        return discount2.get(pattern);
    }

    public double getDiscount3p(Pattern pattern) {
        return discount3p.get(pattern);
    }

}