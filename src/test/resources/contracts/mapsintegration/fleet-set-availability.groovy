import org.springframework.cloud.contract.spec.Contract

/*
 * POST /api/fleet/availability — sets availability in EITHER direction, honouring `available`.
 *
 * available=false is how a release is undone: rejectOrderPing releases the driver before writing
 * its outbox event, and when that write fails it restores the pending ping and re-reserves here.
 * /api/fleet/release cannot serve that case because it ignores the flag.
 *
 * The `false` value is pinned as an exact producer value rather than a matcher, because the
 * direction is the entire point of this endpoint existing separately from /api/fleet/release.
 */
Contract.make {
    description("Should take a driver out of the availability pool when available is false")
    request {
        method 'POST'
        urlPath('/api/fleet/availability')
        headers {
            contentType(applicationJson())
        }
        body([
            cityId  : $(consumer(regex('[A-Za-z0-9_\\-]{1,50}')), producer('BLR')),
            driverId: $(consumer(regex('[0-9a-fA-F\\-]{36}')), producer('4f4a4e37-6ca5-5598-94f1-43ef1628f631')),
            available: false
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
