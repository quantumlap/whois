package net.ripe.db.whois.internal.api.abusec;

import com.google.common.collect.Lists;
import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.internal.api.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Component
@Path("/abusec")
public class AbuseCService {

    public static final String ABUSE_CSERVICE = "abuseCService";
    private final RestClient restClient;
    private String override;

    @Autowired
    public AbuseCService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Value("${api.rest.override}")
    public void setOverride(String override) {
        this.override = override;
    }

    @POST
    @Path("/{orgkey}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createAbuseRole(
            @PathParam("orgkey") final String orgkey,
            @FormParam("email") final String email) {

        RpslObject organisation = null;
        try {
            organisation = restClient.lookup(ObjectType.ORGANISATION, orgkey);
        } catch (Exception e) {
            // TODO: check for specific exception
            e.printStackTrace();
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            final CIString abuseContact = lookupAbuseMailbox(organisation);
            return Response.status(Response.Status.CONFLICT).entity(abuseContact.toString()).build();
        } catch (IllegalArgumentException e) {
            // abuse mailbox not found, continue
        }

        final RpslObject role = createAbuseCRole(organisation, email);

        final RpslObject createdRole = restClient.create(role, String.format("%s,%s", override, ABUSE_CSERVICE));

        final RpslObject updatedOrganisation = createOrganisationWithAbuseCAttribute(organisation, createdRole.getKey().toString());

        restClient.update(updatedOrganisation, String.format("%s,%s", override, ABUSE_CSERVICE));

        return Response.ok(String.format("http://apps.db.ripe.net/search/lookup.html?source=ripe&key=%s&type=ORGANISATION", orgkey)).build();
    }

    @GET
    @Path("/{orgkey}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response lookupAbuseContact(@PathParam("orgkey") final String orgKey) {
        try {
            final RpslObject organisation = restClient.lookup(ObjectType.ORGANISATION, orgKey);
            try {
                final CIString abuseMailbox = lookupAbuseMailbox(organisation);
                return Response.ok(abuseMailbox.toString()).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: check for specific exception
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private CIString lookupAbuseMailbox(final RpslObject organisation) {
        final String abuseRoleName = organisation.getValueForAttribute(AttributeType.ABUSE_C).toString();
        final RpslObject abuseRole = restClient.lookup(ObjectType.ROLE, abuseRoleName);
        return abuseRole.getValueForAttribute(AttributeType.ABUSE_MAILBOX);
    }

    private RpslObject createAbuseCRole(final RpslObject organisation, final String abuseMailbox) {
        final List<RpslAttribute> attributes = Lists.newArrayList();
        attributes.add(new RpslAttribute(AttributeType.ROLE, "Abuse-c Role"));
        attributes.add(new RpslAttribute(AttributeType.NIC_HDL, "AUTO-1"));
        attributes.add(new RpslAttribute(AttributeType.ABUSE_MAILBOX, abuseMailbox));
        for (RpslAttribute mntRef : organisation.findAttributes(AttributeType.MNT_REF)) {
            attributes.add(new RpslAttribute(AttributeType.MNT_BY, mntRef.getValue()));
        }
        for (RpslAttribute address : organisation.findAttributes(AttributeType.ADDRESS)) {
            attributes.add(address);
        }
        final RpslAttribute email = organisation.findAttribute(AttributeType.E_MAIL);
        attributes.add(organisation.findAttribute(AttributeType.E_MAIL));
        attributes.add(new RpslAttribute(AttributeType.CHANGED, email.getValue()));
        final RpslAttribute source = organisation.findAttribute(AttributeType.SOURCE);
        attributes.add(source);
        return new RpslObject(attributes);
    }

    private RpslObject createOrganisationWithAbuseCAttribute(final RpslObject organisation, final String abusec) {
        final List<RpslAttribute> attributes = Lists.newArrayList(organisation.getAttributes());
        attributes.add(new RpslAttribute(AttributeType.ABUSE_C, abusec));
        return new RpslObject(organisation, attributes);
    }
}
