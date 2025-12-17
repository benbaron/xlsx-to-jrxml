package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.EXPENSE_DTL_12aBean;

/** Skeleton generator for JRXML template EXPENSE_DTL_12a.jrxml */
public class EXPENSE_DTL_12aJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<EXPENSE_DTL_12aBean> getReportData()
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
        // TODO return the classpath or filesystem path to EXPENSE_DTL_12a.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "EXPENSE_DTL_12a";
    }
}
