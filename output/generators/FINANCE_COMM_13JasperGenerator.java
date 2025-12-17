package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.FINANCE_COMM_13Bean;

/** Skeleton generator for JRXML template FINANCE_COMM_13.jrxml */
public class FINANCE_COMM_13JasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<FINANCE_COMM_13Bean> getReportData()
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
        // TODO return the classpath or filesystem path to FINANCE_COMM_13.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "FINANCE_COMM_13";
    }
}
