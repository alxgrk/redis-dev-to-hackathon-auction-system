import { toParams, toQuery } from './utils';

class PopupWindow {

    private window?: WindowProxy = null
    private promise?: Promise<any> = null
    private _iid?: number = null

    constructor(private id, private url, private options = {}) {}

    open() {
        const { url, id, options } = this;

        this.window = window.open(url, id, toQuery({popup: true, ...options}, ','));
    }

    close() {
        this.cancel();
        this.window?.close();
    }

    poll() {
        this.promise = new Promise((resolve, reject) => {
            this._iid = window.setInterval(() => {
                try {
                    const popup = this.window;

                    if (!popup || popup.closed !== false) {
                        this.close();

                        reject(new Error('The popup was closed'));

                        return;
                    }

                    if (popup.location.href === this.url || popup.location.pathname === 'blank') {
                        return;
                    }

                    const params = toParams(popup.location.search.replace(/^\?/, ''));

                    resolve(params);

                    this.close();
                } catch (error) {
                    /*
                     * Ignore DOMException: Blocked a frame with origin from accessing a
                     * cross-origin frame.
                     */
                }
            }, 500);
        });
    }

    cancel() {
        if (this._iid) {
            window.clearInterval(this._iid);
            this._iid = null;
        }
    }

    then(...args) {
        return this.promise.then(...args);
    }

    catch(...args) {
        return this.promise.then(...args);
    }

    static open(id, url, options) {
        const popup = new this(id, url, options);

        popup.open();
        popup.poll();

        return popup;
    }
}

export default PopupWindow;
