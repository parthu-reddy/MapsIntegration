import org.springframework.cloud.contract.spec.Contract

/*
 * POST /api/fleet/release — restores a driver's availability after a dispatch ends.
 *
 * Note what this contract deliberately does NOT promise: that `available` is honoured. The endpoint
 * reads only cityId and driverId and always releases, even though SetAvailabilityRequest marks
 * `available` @NotNull so it must still be present. Callers that need the other direction use
 * /api/fleet/availability; see fleet-set-availability.groovy.
 *
 * This is the only path a timed-out or declining driver returns to the pool by — candidate
 * selection removes them with an atomic SREM — so a break here strands drivers silently.
 */
Contract.make {
    description("Should release a driver back into the availability pool")
    request {
        method 'POST'
        urlPath('/api/fleet/release')
        headers {
            contentType(applicationJson())
        }
        body([
            cityId  : $(consumer(regex('[A-Za-z0-9_\\-]{1,50}')), producer('BLR')),
            driverId: $(consumer(regex('[0-9a-fA-F\\-]{36}')), producer('4f4a4e37-6ca5-5598-94f1-43ef1628f631')),
            available: true
        ])
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            success: true
        ])
    }
}
