import { Component } from 'react'

// Shows render errors on screen instead of a blank page.
export default class ErrorBoundary extends Component {
  state = { error: null }
  static getDerivedStateFromError(error) { return { error } }
  render() {
    if (!this.state.error) return this.props.children
    return (
      <div className="error">
        <strong>UI crashed:</strong> {String(this.state.error.message || this.state.error)}
        <pre style={{ whiteSpace: 'pre-wrap' }}>{this.state.error.stack}</pre>
      </div>
    )
  }
}
