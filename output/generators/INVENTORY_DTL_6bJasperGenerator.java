package nonprofitbookkeeping.reports.jasper;

import nonprofitbookkeeping.exception.ActionCancelledException;
import nonprofitbookkeeping.exception.NoFileCreatedException;
import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.beans.INVENTORY_DTL_6bBean;

/** Skeleton generator for JRXML template INVENTORY_DTL_6b.jrxml */
public class INVENTORY_DTL_6bJasperGenerator extends AbstractReportGenerator
{
    @Override
    protected List<INVENTORY_DTL_6bBean> getReportData()
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
        // TODO return the classpath or filesystem path to INVENTORY_DTL_6b.jrxml
        return bundledReportPath();
    }

    @Override
    public String getBaseName()
    {
        return "INVENTORY_DTL_6b";
    }
}
