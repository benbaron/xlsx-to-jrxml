package nonprofitbookkeeping.reports.jasper.generator;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nonprofitbookkeeping.reports.jasper.beans.FUNDS_14Bean;

/** Skeleton generator for JRXML template FUNDS_14.jrxml */
public class FUNDS_14JasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<FUNDS_14Bean> getReportData()
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
        // TODO return the classpath or filesystem path to FUNDS_14.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "FUNDS_14";
    }
}
