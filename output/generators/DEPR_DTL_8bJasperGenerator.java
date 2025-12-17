package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.DEPR_DTL_8bBean;

/** Skeleton generator for JRXML template DEPR_DTL_8b.jrxml */
public class DEPR_DTL_8bJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<DEPR_DTL_8bBean> getReportData()
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
        // TODO return the classpath or filesystem path to DEPR_DTL_8b.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "DEPR_DTL_8b";
    }
}
