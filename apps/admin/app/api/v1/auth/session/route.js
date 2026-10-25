import {adminToken} from '../../../../../lib/upstream';
import {accessMaxAge, sessionVersion} from '../../../../../lib/refresh-session';

export const dynamic = 'force-dynamic';

export function GET(request) {
    return Response.json({
        remaining: accessMaxAge(adminToken(request)),
        version: sessionVersion(adminToken(request))
    }, {headers: {'Cache-Control': 'no-store'}});
}
