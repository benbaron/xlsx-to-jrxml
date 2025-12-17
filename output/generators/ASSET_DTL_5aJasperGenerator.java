package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.ASSET_DTL_5aBean;

/** Skeleton generator for JRXML template ASSET_DTL_5a.jrxml */
public class ASSET_DTL_5aJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<ASSET_DTL_5aBean> getReportData()
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
        // TODO return the classpath or filesystem path to ASSET_DTL_5a.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "ASSET_DTL_5a";
    }
}
