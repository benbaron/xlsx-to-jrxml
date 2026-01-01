package nonprofitbookkeeping.reports.jasper.generator;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nonprofitbookkeeping.reports.jasper.beans.LIABILITY_DTL_5dBean;

/** Skeleton generator for JRXML template LIABILITY_DTL_5d.jrxml */
public class LIABILITY_DTL_5dJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<LIABILITY_DTL_5dBean> getReportData()
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
        // TODO return the classpath or filesystem path to LIABILITY_DTL_5d.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "LIABILITY_DTL_5d";
    }
}
