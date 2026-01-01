package nonprofitbookkeeping.reports.jasper.generator;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nonprofitbookkeeping.reports.jasper.beans.INCOME_4Bean;

/** Skeleton generator for JRXML template INCOME_4.jrxml */
public class INCOME_4JasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<INCOME_4Bean> getReportData()
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
        // TODO return the classpath or filesystem path to INCOME_4.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "INCOME_4";
    }
}
