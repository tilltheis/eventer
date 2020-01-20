import React from 'react';

export default class EventList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      'title': '',
      'date': '',
      'time': '',
      'description': ''
    };

    this.handleInputChange = this.handleInputChange.bind(this);
  }

  handleInputChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;

    this.setState({
      [name]: value
    });
  }

  render() {
    return (
      <>
        <h2>Creating Event</h2>
        <form method="post">
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input className="form-control" id="title" name="title" value={this.state.title} onChange={this.handleInputChange} required />
          </div>

          <div className="form-row align-items-center form-group">
            <div className="col">
              <label htmlFor="date">Date</label>
              <input className="form-control" id="date" name="date" type="date" value={this.state.date} onChange={this.handleInputChange} required />
            </div>

            <div className="col">
              <label htmlFor="time">Time</label>
              <input className="form-control" id="time" name="time" type="time" value={this.state.time} onChange={this.handleInputChange} required />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="guestIds">Guests</label>
            <select className="form-control" id="guestIds" name="guestIds[]" multiple>
              <option disabled>Select Guests</option>
              <option value="id">Guest Name</option>
            </select>
          </div>

          <div className="form-row align-items-center form-group">
            <div className="col">
                <label htmlFor="guestName">External Guest Name</label>
                <input className="form-control" id="guestName" />
            </div>
            <div className="col">
                <label htmlFor="guestEmail">External Guest Email</label>
                <input className="form-control" id="guestEmail" type="email" />
            </div>
            <div className="col">
              <label>&nbsp;</label>
              <button className="btn btn-outline-secondary form-control" id="addGuestEmail">Add External Guest</button>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea className="form-control" id="description" name="description" value={this.state.description} onChange={this.handleInputChange} />
          </div>
          <input className="btn btn-primary" type="submit" value="Create Event" />
        </form>
      </>
    );
  }
};