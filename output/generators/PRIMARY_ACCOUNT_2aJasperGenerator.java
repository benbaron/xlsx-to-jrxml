package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.PRIMARY_ACCOUNT_2aBean;

/** Skeleton generator for JRXML template PRIMARY_ACCOUNT_2a.jrxml */
public class PRIMARY_ACCOUNT_2aJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<PRIMARY_ACCOUNT_2aBean> getReportData()
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
        // TODO return the classpath or filesystem path to PRIMARY_ACCOUNT_2a.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "PRIMARY_ACCOUNT_2a";
    }
}
