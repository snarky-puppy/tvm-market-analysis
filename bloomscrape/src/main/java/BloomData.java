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
    public BloomData acquiredBy;
    public boolean delisted = false;
    public boolean changed = false;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append("").append(code);
        if(symbol == null)
            return sb.toString();

        sb.append(",").append(symbol.replaceAll(":[A-Z]*$", ""));
        sb.append(",").append(ticker);
        sb.append(",").append(companyName);
        sb.append(",").append(market);
        sb.append(",").append(sector);
        sb.append(",").append(industry);
        sb.append(",").append(subIndustry);
        sb.append(",").append(acquiredBy);
        sb.append(",").append(delisted);

        return sb.toString();
    }
}
