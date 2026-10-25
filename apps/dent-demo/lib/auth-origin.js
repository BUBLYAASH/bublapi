export function publicOrigin(request) {
    const configured = process.env.PUBLIC_ORIGIN;
    return configured ? new URL(configured).origin : new URL(request.url).origin;
}

export function sameOrigin(request) {
    const origin = request.headers.get('origin');
    if (!origin || origin === 'null') return false;
    try {
        return new URL(origin).origin === publicOrigin(request) && origin === new URL(origin).origin;
    } catch {
        return false;
    }
}

export function backendOrigin(request) {
    // Never fabricate an allowed localhost Origin for a production caller.
    return publicOrigin(request);
}
