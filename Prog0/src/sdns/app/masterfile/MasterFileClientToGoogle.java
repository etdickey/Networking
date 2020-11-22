//Contains the MasterFileClientToGoogle class (see comments below)
//Created: 10/20/20
package sdns.app.masterfile;

import sdns.app.tcp.client.Client;
import sdns.serialization.ResourceRecord;
import sdns.serialization.Response;
import sdns.serialization.ValidationException;
import sdns.serialization.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * SDNS Response RRs source
 * @author Ethan Dickey
 *  Credit: Dr. Donahoo of Baylor University for specifications and api
 * @version 1.0
 */
public class MasterFileClientToGoogle implements MasterFile {
    private static final String DNS_IP_ADDR = "ns3.baylor.edu";//local is "66.90.134.62"
    /**
     * Populate answer, name server, and additional list RRs.
     *
     * @param question    query for SDNS query
     * @param answers     RR list (allocated) to add answer RRs to
     * @param nameservers RR list (allocated) to add name server RRs to
     * @param additionals RR list (allocated) to add additional RRs to
     * @throws NoSuchElementException if no such domain name
     * @throws NullPointerException   if any parameters are null
     * @throws ValidationException    if question is invalid or anything else goes wrong while
     *                                trying to resolve question
     */
    @Override
    public void search(String question, List<ResourceRecord> answers, List<ResourceRecord> nameservers, List<ResourceRecord> additionals) throws NoSuchElementException, NullPointerException, ValidationException {
        ValidationUtils.validateDomainName(question);
        Objects.requireNonNull(answers);
        Objects.requireNonNull(nameservers);
        Objects.requireNonNull(additionals);

        //connect to DNS server with request
        try {
            List<Response> responses = new ArrayList<>();
            Client.runClient(new String[]{DNS_IP_ADDR, "53", question}, true, responses);

            //this is relatively hacky but we can get away with it
            //we only asked one question so the response better be the first one in the list
            if(responses.size() > 0){
                for(ResourceRecord rr : responses.get(0).getAnswerList()){
                    answers.add((ResourceRecord) rr.clone());
                }
                for(ResourceRecord rr : responses.get(0).getNameServerList()){
                    nameservers.add((ResourceRecord) rr.clone());
                }
                for(ResourceRecord rr : responses.get(0).getAdditionalList()){
                    additionals.add((ResourceRecord) rr.clone());
                }
            } else {
                throw new NoSuchElementException("No responses");
            }
        } catch(Exception e){
            throw new NoSuchElementException("Error running client: " + e.getMessage());
        }
    }
}
