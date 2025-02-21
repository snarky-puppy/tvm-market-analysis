/**
 * Data bean
 *
 * Created by matt on 25/10/16.
 */
class BloomData {
    public int code;
    public String symbol;
    public String ticker;
    public String companyName;
    public String market;
    public String sector;
    public String industry;
    public String subIndustry;
    public Boolean delisted;
    public Boolean changed;
    public BloomData acquiredBy;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append("").append(code);
        if(symbol == null)
            return sb.toString();

        nice(sb, symbol.replaceAll(":[A-Z]*$", ""));
        nice(sb, ticker);
        nice(sb, companyName);
        nice(sb, market);
        nice(sb, sector);
        nice(sb, industry);
        nice(sb, subIndustry);
        nice(sb, delisted);
        nice(sb, changed);
        if(acquiredBy != null)
            sb.append("\t").append(acquiredBy);

        return sb.toString();
    }
    
    void nice(StringBuffer sb, String s) {
        sb.append("\t");
        if(s != null)
            sb.append(s);
    }

    void nice(StringBuffer sb, Boolean s) {
        sb.append("\t");
        if(s != null)
            sb.append(s);
    }
}
