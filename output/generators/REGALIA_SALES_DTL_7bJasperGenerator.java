package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.REGALIA_SALES_DTL_7bBean;

/** Skeleton generator for JRXML template REGALIA_SALES_DTL_7b.jrxml */
public class REGALIA_SALES_DTL_7bJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<REGALIA_SALES_DTL_7bBean> getReportData()
    {
        // TODO supply data beans for the report
        return Collections.emptyList();
    }

    @Override
    protected Map<String, Object> getReportParameters()
    {
        Map<String, Object> params = new HashMap<>();
        // TODO populate report parameters such as title or filters
        return params;
    }

    @Override
    protected String getReportPath() throws ActionCancelledException, NoFileCreatedException
    {
        // TODO return the classpath or filesystem path to REGALIA_SALES_DTL_7b.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "REGALIA_SALES_DTL_7b";
    }
}
