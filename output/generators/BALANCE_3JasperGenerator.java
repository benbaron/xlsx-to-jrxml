package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.BALANCE_3Bean;

/** Skeleton generator for JRXML template BALANCE_3.jrxml */
public class BALANCE_3JasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<BALANCE_3Bean> getReportData()
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
        // TODO return the classpath or filesystem path to BALANCE_3.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "BALANCE_3";
    }
}
